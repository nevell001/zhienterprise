/**
 * Banner 图片 object-fit 自适应：
 * 宽幅/竖幅与容器比例差超过约 1.35 倍时改用 contain，避免裁掉画面里的文字；
 * 比例接近时仍用 cover 铺满。
 */
export function pickBannerFit(imageRatio: number, boxRatio: number): 'cover' | 'contain' {
  if (
    !Number.isFinite(imageRatio) ||
    !Number.isFinite(boxRatio) ||
    imageRatio <= 0 ||
    boxRatio <= 0
  ) {
    return 'cover'
  }
  return imageRatio > boxRatio * 1.35 || imageRatio < boxRatio / 1.35 ? 'contain' : 'cover'
}
