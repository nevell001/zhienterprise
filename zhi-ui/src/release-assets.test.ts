import { describe, expect, it } from 'vitest'
import { execFileSync, spawnSync } from 'node:child_process'
import {
  chmodSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readdirSync,
  readFileSync,
  rmSync,
  writeFileSync
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

/**
 * 发布资产 ↔ 数据库补丁 ↔ 运维脚本的跨产物契约。
 *
 * 注：这里刻意做源码级断言。它比较的是「发布流水线是否把 99 补丁和 scripts 打进包」以及
 * 「三个脚本的调用顺序/关键命令」—— 这类问题只在客户拿到 release 包后才会暴露，无法用
 * 组件行为测试覆盖（与 nginx.config.test.ts 同类）。
 */

const ROOT = resolve(process.cwd(), '..')
const read = (path: string) => readFileSync(resolve(ROOT, path), 'utf-8')

const RELEASE_WORKFLOWS = ['.github/workflows/release.yml', '.workflow/ReleasePipeline.yml']
const SCRIPTS = ['common.sh', 'backup.sh', 'restore.sh', 'upgrade.sh']

/**
 * 用假 docker 跑真实脚本：验证产物/恢复/升级顺序，而不是只 grep 脚本文本。
 * 假 docker 只实现 compose ps / exec / stop / up / restart 这几个被脚本用到的子命令。
 */
const FAKE_DOCKER = `#!/usr/bin/env bash
set -euo pipefail
printf '%s\\n' "$*" >> "\${FAKE_LOG:?}"
if [ "\${1:-}" != "compose" ]; then exit 0; fi
shift
if [ "\${1:-}" = "-f" ]; then shift 2; fi
cmd="\${1:-}"; shift || true
case "$cmd" in
  version) exit 0 ;;
  ps) echo fake-mysql-cid ;;
  exec)
    while [ "$#" -gt 0 ]; do
      case "$1" in
        -T) shift ;;
        -e) shift 2 ;;
        *) break ;;
      esac
    done
    service="\${1:-}"; shift || true
    tool="\${1:-}"; shift || true
    if [ "$service" = "mysql" ] && [ "$tool" = "mysqldump" ]; then
      printf -- '-- fake dump\\nCREATE TABLE t (id int);\\n'
    elif [ "$service" = "mysql" ] && [ "$tool" = "mysql" ]; then
      cat > "\${FAKE_RESTORE_OUT:?}"
    fi
    ;;
  *) : ;;
esac
`

function sandbox(envContent = 'DB_NAME=testdb\nDB_ROOT_PASSWORD=secret\n') {
  const dir = mkdtempSync(join(tmpdir(), 'zhienterprise-scripts-'))
  const bin = join(dir, 'bin')
  mkdirSync(bin)
  mkdirSync(join(dir, 'uploadPath'))
  writeFileSync(join(dir, 'uploadPath', 'a.txt'), 'upload')
  writeFileSync(join(dir, '.env'), envContent)
  writeFileSync(join(dir, 'docker-compose.release.yml'), 'services: {}\n')
  const docker = join(bin, 'docker')
  writeFileSync(docker, FAKE_DOCKER)
  chmodSync(docker, 0o755)
  return { dir, calls: join(dir, 'calls.log'), restored: join(dir, 'restored.sql') }
}

function runScript(
  script: string,
  args: string[],
  box: ReturnType<typeof sandbox>,
  extraEnv: Record<string, string> = {}
) {
  return spawnSync('bash', [resolve(ROOT, 'scripts', script), ...args], {
    env: {
      ...process.env,
      APP_DIR: box.dir,
      BACKUP_DIR: join(box.dir, 'backups'),
      PATH: `${join(box.dir, 'bin')}:${process.env.PATH}`,
      FAKE_LOG: box.calls,
      FAKE_RESTORE_OUT: box.restored,
      ...extraEnv
    },
    encoding: 'utf-8'
  })
}

describe('发布包必须携带全部数据库补丁与运维脚本', () => {
  it.each(RELEASE_WORKFLOWS)('%s 打包整个 sql/ 与 scripts/ 目录', file => {
    const source = read(file)

    // 整目录打包，后续新增 99_*.sql 不需要再改流水线
    expect(source).toContain('sql deploy-assets/sql')
    expect(source).toContain('scripts deploy-assets/scripts')
    expect(source).toContain('docs/BACKUP_RESTORE.md')
  })

  it('sql/ 下存在 00 初始化脚本', () => {
    expect(existsSync(resolve(ROOT, 'sql', '00_init_database.sql'))).toBe(true)
  })

  it.each(SCRIPTS)('scripts/%s 语法合法且随包存在', script => {
    const path = resolve(ROOT, 'scripts', script)

    expect(existsSync(path)).toBe(true)
    expect(() => execFileSync('bash', ['-n', path], { stdio: 'pipe' })).not.toThrow()
  })

  it('CI 会对脚本做语法检查', () => {
    expect(read('.github/workflows/ci.yml')).toContain('bash -n "$f"')
  })
})

describe('备份 / 恢复 / 升级脚本的关键约定', () => {
  it('备份包含数据库、上传目录与校验和', () => {
    const source = read('scripts/backup.sh')

    expect(source).toContain('mysqldump')
    expect(source).toContain('uploadPath.tar.gz')
    expect(source).toContain('write_checksums')
  })

  it('恢复前校验备份、停应用，并使用恢复前安全备份', () => {
    const source = read('scripts/restore.sh')

    expect(source).toContain('verify_checksums')
    expect(source).toContain('compose stop zhi-admin zhi-ui')
    expect(source).toContain('backup.sh')
    expect(source).toContain('gunzip -c')
  })

  it('升级顺序必须是 00 → 其余 99', () => {
    const source = read('scripts/upgrade.sh')
    const initIndex = source.indexOf('run_sql "$SQL_DIR/00_init_database.sql"')
    const remainingIndex = source.indexOf('find "$SQL_DIR" -maxdepth 1 -type f -name \'99_*.sql\'')

    expect(initIndex).toBeGreaterThan(-1)
    expect(remainingIndex).toBeGreaterThan(initIndex)
  })
})

describe('运维脚本行为（假 docker）', () => {
  it('backup.sh 产出数据库/上传目录/清单/校验和，并可通过校验', () => {
    const box = sandbox()
    const result = runScript('backup.sh', [], box)

    expect(result.status, result.stderr).toBe(0)
    const backups = readdirSync(join(box.dir, 'backups')).filter(name => name.startsWith('backup-'))
    expect(backups).toHaveLength(1)
    const dir = join(box.dir, 'backups', backups[0])

    expect(execFileSync('gzip', ['-dc', join(dir, 'db.sql.gz')], { encoding: 'utf-8' })).toContain(
      'fake dump'
    )
    expect(
      execFileSync('tar', ['-tzf', join(dir, 'uploadPath.tar.gz')], { encoding: 'utf-8' })
    ).toContain('uploadPath/a.txt')
    expect(readFileSync(join(dir, 'manifest.txt'), 'utf-8')).toContain('db_name=testdb')
    expect(() =>
      execFileSync(
        'bash',
        [
          '-c',
          'command -v sha256sum >/dev/null && sha256sum -c SHA256SUMS || shasum -a 256 -c SHA256SUMS'
        ],
        {
          cwd: dir,
          stdio: 'pipe'
        }
      )
    ).not.toThrow()
  })

  it('restore.sh 停应用、校验备份并恢复数据库与上传目录', () => {
    const box = sandbox()
    expect(runScript('backup.sh', [], box).status).toBe(0)
    const backupName = readdirSync(join(box.dir, 'backups')).find(name =>
      name.startsWith('backup-')
    )!
    const backupDir = join(box.dir, 'backups', backupName)

    rmSync(join(box.dir, 'uploadPath', 'a.txt'))
    const result = runScript('restore.sh', [backupDir, '--yes'], box, {
      SKIP_SAFETY_BACKUP: '1'
    })

    expect(result.status, result.stderr).toBe(0)
    expect(readFileSync(box.restored, 'utf-8')).toContain('fake dump')
    expect(readFileSync(join(box.dir, 'uploadPath', 'a.txt'), 'utf-8')).toBe('upload')

    const calls = readFileSync(box.calls, 'utf-8')
    expect(calls).toContain('stop zhi-admin zhi-ui')
    expect(calls).toContain('up -d zhi-admin zhi-ui')
  })

  it('upgrade.sh 按 00 → 其余 99 执行并重启后端', () => {
    const box = sandbox()
    const sqlDir = join(box.dir, 'sql')
    mkdirSync(sqlDir)
    for (const name of ['00_init_database.sql', '99_z_fix.sql']) {
      writeFileSync(join(sqlDir, name), `-- ${name}\nSELECT 1;\n`)
    }

    const result = runScript('upgrade.sh', ['--yes', '--no-backup'], box)

    expect(result.status, result.stderr).toBe(0)
    const init = result.stdout.indexOf('执行 00_init_database.sql')
    const patch = result.stdout.indexOf('执行 99_z_fix.sql')
    expect(init).toBeGreaterThan(-1)
    expect(patch).toBeGreaterThan(init)
    expect(readFileSync(box.calls, 'utf-8')).toContain('stop zhi-admin')
  })
})
