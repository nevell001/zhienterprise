/**
 * 注入站点「统计代码」。
 *
 * template.innerHTML 解析出的 script 不会执行，因此这里显式重建 script 节点再挂载；
 * 所有节点收进一个包装容器，返回的清理函数可整块移除（切换设置/离开前台时）。
 */
export function injectCustomJs(code: string, container: HTMLElement = document.body): () => void {
  const source = code?.trim()
  if (!source) return () => {}

  const template = document.createElement('template')
  template.innerHTML = source
  const wrapper = document.createElement('div')
  wrapper.setAttribute('data-custom-js', '')
  container.appendChild(wrapper)

  Array.from(template.content.childNodes).forEach(node => {
    if (node.nodeType === Node.ELEMENT_NODE && (node as Element).tagName === 'SCRIPT') {
      const original = node as HTMLScriptElement
      const script = document.createElement('script')
      Array.from(original.attributes).forEach(attr => script.setAttribute(attr.name, attr.value))
      script.text = original.text
      wrapper.appendChild(script)
      return
    }
    wrapper.appendChild(node.cloneNode(true))
  })

  return () => wrapper.remove()
}
