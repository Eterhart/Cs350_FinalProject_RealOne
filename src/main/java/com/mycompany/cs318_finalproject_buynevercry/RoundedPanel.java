package com.mycompany.cs318_finalproject_buynevercry;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.awt.Insets;

public class RoundedPanel extends JPanel {

    // ---------- OOP: แยก Style ----------
    public static final class Style {

        // legacy (ยังใช้ได้): ถ้าตั้งตัวนี้ จะกระจายไป 4 มุม
        private int cornerRadius = 12;

        // new: แยก 4 มุม
        private int topLeft = 12;
        private int topRight = 12;
        private int bottomRight = 12;
        private int bottomLeft = 12;

        private int opacityPercent = 100;           // 0–100
        private Color panelColor = Color.WHITE;

        private Color borderColor = new Color(213, 213, 213);
        private float borderThickness = 1.0f;

        // ===== image support =====
        private Icon imageIcon = null;
        private boolean imageEnabled = true;
        private boolean imageCover = false;         // false=FIT, true=COVER
        private int imageOpacityPercent = 100;      // 0–100

        // ===== shadow support =====
        private boolean shadowEnabled = false;
        private Color shadowColor = new Color(0, 0, 0);
        private int shadowOpacityPercent = 25;      // 0–100 (ความเข้ม)
        private int shadowBlurRadius = 12;          // 0–50 (ความฟุ้ง)
        private int shadowOffsetX = 0;              // เลื่อนไปซ้าย/ขวา
        private int shadowOffsetY = 6;              // เลื่อนขึ้น/ลง

        // ----- legacy cornerRadius -----
        public int getCornerRadius() { return cornerRadius; }
        public void setCornerRadius(int v) {
            cornerRadius = Math.max(0, v);
            topLeft = cornerRadius;
            topRight = cornerRadius;
            bottomRight = cornerRadius;
            bottomLeft = cornerRadius;
        }

        // ----- 4 corners -----
        public int getTopLeft() { return topLeft; }
        public void setTopLeft(int v) { topLeft = Math.max(0, v); syncCornerRadiusIfAllEqual(); }

        public int getTopRight() { return topRight; }
        public void setTopRight(int v) { topRight = Math.max(0, v); syncCornerRadiusIfAllEqual(); }

        public int getBottomRight() { return bottomRight; }
        public void setBottomRight(int v) { bottomRight = Math.max(0, v); syncCornerRadiusIfAllEqual(); }

        public int getBottomLeft() { return bottomLeft; }
        public void setBottomLeft(int v) { bottomLeft = Math.max(0, v); syncCornerRadiusIfAllEqual(); }

        private void syncCornerRadiusIfAllEqual() {
            if (topLeft == topRight && topRight == bottomRight && bottomRight == bottomLeft) {
                cornerRadius = topLeft;
            }
        }

        // ----- opacity -----
        public int getOpacityPercent() { return opacityPercent; }
        public void setOpacityPercent(int v) { opacityPercent = clamp(v, 0, 100); }

        // ----- colors -----
        public Color getPanelColor() { return panelColor; }
        public void setPanelColor(Color c) { panelColor = (c == null) ? Color.WHITE : c; }

        public Color getBorderColor() { return borderColor; }
        public void setBorderColor(Color c) { borderColor = (c == null) ? new Color(0,0,0,0) : c; }

        // ----- border thickness -----
        public float getBorderThickness() { return borderThickness; }
        public void setBorderThickness(float t) { borderThickness = Math.max(0f, t); }

        // ----- image -----
        public Icon getImageIcon() { return imageIcon; }
        public void setImageIcon(Icon icon) { imageIcon = icon; }

        public boolean isImageEnabled() { return imageEnabled; }
        public void setImageEnabled(boolean enabled) { imageEnabled = enabled; }

        public boolean isImageCover() { return imageCover; }
        public void setImageCover(boolean cover) { imageCover = cover; }

        public int getImageOpacityPercent() { return imageOpacityPercent; }
        public void setImageOpacityPercent(int v) { imageOpacityPercent = clamp(v, 0, 100); }

        // ----- shadow -----
        public boolean isShadowEnabled() { return shadowEnabled; }
        public void setShadowEnabled(boolean v) { shadowEnabled = v; }

        public Color getShadowColor() { return shadowColor; }
        public void setShadowColor(Color c) { shadowColor = (c == null) ? new Color(0,0,0) : c; }

        public int getShadowOpacityPercent() { return shadowOpacityPercent; }
        public void setShadowOpacityPercent(int v) { shadowOpacityPercent = clamp(v, 0, 100); }

        public int getShadowBlurRadius() { return shadowBlurRadius; }
        public void setShadowBlurRadius(int v) { shadowBlurRadius = clamp(v, 0, 50); }

        public int getShadowOffsetX() { return shadowOffsetX; }
        public void setShadowOffsetX(int v) { shadowOffsetX = v; }

        public int getShadowOffsetY() { return shadowOffsetY; }
        public void setShadowOffsetY(int v) { shadowOffsetY = v; }

        // helpers
        private static int clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }

        public Color getFillWithAlpha() {
            int alpha = (int) Math.round(opacityPercent * 2.55);
            return new Color(panelColor.getRed(), panelColor.getGreen(), panelColor.getBlue(), alpha);
        }

        public float getImageAlpha01() {
            return imageOpacityPercent / 100f;
        }

        public float getShadowAlpha01() {
            return shadowOpacityPercent / 100f;
        }
    }

    // ---------- Component ----------
    private final Style style = new Style();

    // cache เงา (กันคำนวณ blur ทุกเฟรม)
    private BufferedImage shadowCache;
    private int shadowCacheW = -1, shadowCacheH = -1;
    private int shadowCacheBlur = -1;
    private int shadowCacheTL = -1, shadowCacheTR = -1, shadowCacheBR = -1, shadowCacheBL = -1;
    private int shadowCacheOffsetX = Integer.MIN_VALUE, shadowCacheOffsetY = Integer.MIN_VALUE;
    private int shadowCacheOpacity = -1;
    private Color shadowCacheColor = null;

    public RoundedPanel() {
        setOpaque(false);
    }

    public Style getStyle() {
        return style;
    }

    // ===== เผื่อพื้นที่ให้เงา (Insets) =====
    private Insets getShadowInsets() {
        if (!style.isShadowEnabled() || style.getShadowOpacityPercent() <= 0) {
            return new Insets(0, 0, 0, 0);
        }

        int blur = Math.max(0, style.getShadowBlurRadius());
        int ox = style.getShadowOffsetX();
        int oy = style.getShadowOffsetY();

        int left   = blur + Math.max(0, -ox);
        int right  = blur + Math.max(0,  ox);
        int top    = blur + Math.max(0, -oy);
        int bottom = blur + Math.max(0,  oy);

        int extra = 2; // กันตัดขอบนิดนึง
        return new Insets(top + extra, left + extra, bottom + extra, right + extra);
    }

    @Override
    public Insets getInsets() {
        Insets base = super.getInsets();
        Insets sh = getShadowInsets();
        return new Insets(
                base.top + sh.top,
                base.left + sh.left,
                base.bottom + sh.bottom,
                base.right + sh.right
        );
    }

    @Override
    public Insets getInsets(Insets insets) {
        Insets i = super.getInsets(insets);
        Insets sh = getShadowInsets();
        i.top += sh.top;
        i.left += sh.left;
        i.bottom += sh.bottom;
        i.right += sh.right;
        return i;
    }

    // ---------- JavaBean properties สำหรับ NetBeans ----------
    public int getCornerRadius() { return style.getCornerRadius(); }
    public void setCornerRadius(int v) { style.setCornerRadius(v); invalidateShadow(); repaint(); }

    public int getOpacityPercent() { return style.getOpacityPercent(); }
    public void setOpacityPercent(int v) { style.setOpacityPercent(v); repaint(); }

    public Color getPanelColor() { return style.getPanelColor(); }
    public void setPanelColor(Color c) { style.setPanelColor(c); repaint(); }

    public Color getBorderColor() { return style.getBorderColor(); }
    public void setBorderColor(Color c) { style.setBorderColor(c); repaint(); }

    public float getBorderThickness() { return style.getBorderThickness(); }
    public void setBorderThickness(float t) { style.setBorderThickness(t); invalidateShadow(); repaint(); }

    public int getTopLeft() { return style.getTopLeft(); }
    public void setTopLeft(int v) { style.setTopLeft(v); invalidateShadow(); repaint(); }

    public int getTopRight() { return style.getTopRight(); }
    public void setTopRight(int v) { style.setTopRight(v); invalidateShadow(); repaint(); }

    public int getBottomRight() { return style.getBottomRight(); }
    public void setBottomRight(int v) { style.setBottomRight(v); invalidateShadow(); repaint(); }

    public int getBottomLeft() { return style.getBottomLeft(); }
    public void setBottomLeft(int v) { style.setBottomLeft(v); invalidateShadow(); repaint(); }

    // ----- image -----
    public Icon getImageIcon() { return style.getImageIcon(); }
    public void setImageIcon(Icon icon) { style.setImageIcon(icon); repaint(); }

    public boolean isImageEnabled() { return style.isImageEnabled(); }
    public void setImageEnabled(boolean enabled) { style.setImageEnabled(enabled); repaint(); }

    public boolean isImageCover() { return style.isImageCover(); }
    public void setImageCover(boolean cover) { style.setImageCover(cover); repaint(); }

    public int getImageOpacityPercent() { return style.getImageOpacityPercent(); }
    public void setImageOpacityPercent(int v) { style.setImageOpacityPercent(v); repaint(); }

    // ----- shadow (JavaBean) -----
    public boolean isShadowEnabled() { return style.isShadowEnabled(); }
    public void setShadowEnabled(boolean v) {
        style.setShadowEnabled(v);
        invalidateShadow();
        revalidate(); // สำคัญ: ให้ layout เผื่อพื้นที่ใหม่
        repaint();
    }

    public Color getShadowColor() { return style.getShadowColor(); }
    public void setShadowColor(Color c) {
        style.setShadowColor(c);
        invalidateShadow();
        repaint();
    }

    public int getShadowOpacityPercent() { return style.getShadowOpacityPercent(); }
    public void setShadowOpacityPercent(int v) {
        style.setShadowOpacityPercent(v);
        invalidateShadow();
        revalidate();
        repaint();
    }

    public int getShadowBlurRadius() { return style.getShadowBlurRadius(); }
    public void setShadowBlurRadius(int v) {
        style.setShadowBlurRadius(v);
        invalidateShadow();
        revalidate(); // สำคัญ: insets เปลี่ยน
        repaint();
    }

    public int getShadowOffsetX() { return style.getShadowOffsetX(); }
    public void setShadowOffsetX(int v) {
        style.setShadowOffsetX(v);
        invalidateShadow();
        revalidate(); // สำคัญ: insets เปลี่ยน
        repaint();
    }

    public int getShadowOffsetY() { return style.getShadowOffsetY(); }
    public void setShadowOffsetY(int v) {
        style.setShadowOffsetY(v);
        invalidateShadow();
        revalidate(); // สำคัญ: insets เปลี่ยน
        repaint();
    }

    private void invalidateShadow() {
        shadowCache = null;
    }

    // ---------- สร้าง shape แบบโค้ง 4 มุม ----------
    private Shape createRoundedShape(float x, float y, float w, float h,
                                     float tl, float tr, float br, float bl) {

        float maxR = Math.min(w, h) / 2f;
        tl = Math.min(tl, maxR);
        tr = Math.min(tr, maxR);
        br = Math.min(br, maxR);
        bl = Math.min(bl, maxR);

        Path2D p = new Path2D.Float();

        p.moveTo(x + tl, y);

        p.lineTo(x + w - tr, y);
        p.quadTo(x + w, y, x + w, y + tr);

        p.lineTo(x + w, y + h - br);
        p.quadTo(x + w, y + h, x + w - br, y + h);

        p.lineTo(x + bl, y + h);
        p.quadTo(x, y + h, x, y + h - bl);

        p.lineTo(x, y + tl);
        p.quadTo(x, y, x + tl, y);

        p.closePath();
        return p;
    }

    // ---------- คำนวณการวาดรูปแบบ FIT / COVER ----------
    private Rectangle computeImageRect(int imgW, int imgH, int boxW, int boxH, boolean cover) {
        if (imgW <= 0 || imgH <= 0 || boxW <= 0 || boxH <= 0) return new Rectangle(0, 0, 0, 0);

        double sx = boxW / (double) imgW;
        double sy = boxH / (double) imgH;
        double scale = cover ? Math.max(sx, sy) : Math.min(sx, sy);

        int drawW = (int) Math.round(imgW * scale);
        int drawH = (int) Math.round(imgH * scale);

        int x = (boxW - drawW) / 2;
        int y = (boxH - drawH) / 2;

        return new Rectangle(x, y, drawW, drawH);
    }

    // ===== Shadow helpers (Gaussian blur) =====
    private static Kernel gaussianKernel(int radius) {
        if (radius <= 0) {
            return new Kernel(1, 1, new float[]{1f});
        }

        int size = radius * 2 + 1;
        float[] data = new float[size];
        float sigma = radius / 3f;
        if (sigma < 0.1f) sigma = 0.1f;

        float sum = 0f;
        for (int i = 0; i < size; i++) {
            int x = i - radius;
            float v = (float) Math.exp(-(x * x) / (2f * sigma * sigma));
            data[i] = v;
            sum += v;
        }
        for (int i = 0; i < size; i++) data[i] /= sum;

        return new Kernel(size, 1, data);
    }

    private static BufferedImage blur(BufferedImage src, int radius) {
        if (radius <= 0) return src;

        Kernel k = gaussianKernel(radius);
        ConvolveOp horiz = new ConvolveOp(k, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage tmp = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        horiz.filter(src, tmp);

        ConvolveOp vert = new ConvolveOp(new Kernel(1, k.getWidth(), k.getKernelData(null)), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        vert.filter(tmp, out);

        return out;
    }

    private BufferedImage getShadowImage(int w, int h, Shape shape) {
        int blurR = style.getShadowBlurRadius();
        int opacity = style.getShadowOpacityPercent();
        int ox = style.getShadowOffsetX();
        int oy = style.getShadowOffsetY();
        Color sc = style.getShadowColor();
        int tl = style.getTopLeft(), tr = style.getTopRight(), br = style.getBottomRight(), bl = style.getBottomLeft();

        boolean valid =
                shadowCache != null &&
                shadowCacheW == w && shadowCacheH == h &&
                shadowCacheBlur == blurR &&
                shadowCacheOffsetX == ox && shadowCacheOffsetY == oy &&
                shadowCacheOpacity == opacity &&
                ((shadowCacheColor == null && sc == null) || (shadowCacheColor != null && shadowCacheColor.equals(sc))) &&
                shadowCacheTL == tl && shadowCacheTR == tr && shadowCacheBR == br && shadowCacheBL == bl;

        if (valid) return shadowCache;

        BufferedImage base = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = base.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float a = style.getShadowAlpha01();
        a = Math.max(0f, Math.min(1f, a));
        Color fill = new Color(sc.getRed(), sc.getGreen(), sc.getBlue(), Math.round(a * 255));

        g.setColor(fill);

        // วาดเงา “เยื้อง” ด้วย offset ก่อนค่อย blur
        g.translate(ox, oy);
        g.fill(shape);

        g.dispose();

        BufferedImage blurred = blur(base, blurR);

        shadowCache = blurred;
        shadowCacheW = w; shadowCacheH = h;
        shadowCacheBlur = blurR;
        shadowCacheOffsetX = ox; shadowCacheOffsetY = oy;
        shadowCacheOpacity = opacity;
        shadowCacheColor = sc;
        shadowCacheTL = tl; shadowCacheTR = tr; shadowCacheBR = br; shadowCacheBL = bl;

        return shadowCache;
    }

    // ---------- Rendering ----------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float stroke = style.getBorderThickness();
        float insetStroke = stroke / 2f + 1f;

        Insets pad = getShadowInsets();

        float x0 = pad.left + insetStroke;
        float y0 = pad.top + insetStroke;

        float w = getWidth()  - pad.left - pad.right - insetStroke * 2;
        float h = getHeight() - pad.top  - pad.bottom - insetStroke * 2;

        if (w <= 1 || h <= 1) {
            g2.dispose();
            return;
        }

        float tl = style.getTopLeft();
        float tr = style.getTopRight();
        float br = style.getBottomRight();
        float bl = style.getBottomLeft();

        Shape shape = createRoundedShape(x0, y0, w, h, tl, tr, br, bl);

        // ===== shadow (วาดก่อน) =====
        if (style.isShadowEnabled() && getWidth() > 1 && getHeight() > 1 && style.getShadowOpacityPercent() > 0) {
            BufferedImage sh = getShadowImage(getWidth(), getHeight(), shape);
            g2.drawImage(sh, 0, 0, null);
        }

        // fill
        g2.setColor(style.getFillWithAlpha());
        g2.fill(shape);

        // image (clip)
        Icon icon = style.getImageIcon();
        if (style.isImageEnabled() && icon != null && w > 1 && h > 1) {
            Graphics2D gi = (Graphics2D) g2.create();
            gi.setClip(shape);

            float a = Math.max(0f, Math.min(1f, style.getImageAlpha01()));
            gi.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));

            Image img = null;
            int imgW = icon.getIconWidth();
            int imgH = icon.getIconHeight();

            if (icon instanceof ImageIcon) {
                img = ((ImageIcon) icon).getImage();
            }

            int boxW = Math.round(w);
            int boxH = Math.round(h);
            int bx = Math.round(x0);
            int by = Math.round(y0);

            if (img != null) {
                Rectangle r = computeImageRect(imgW, imgH, boxW, boxH, style.isImageCover());
                gi.drawImage(img, bx + r.x, by + r.y, r.width, r.height, this);
            } else {
                int x = bx + (boxW - imgW) / 2;
                int y = by + (boxH - imgH) / 2;
                icon.paintIcon(this, gi, x, y);
            }

            gi.dispose();
        }

        // border
        if (stroke > 0f) {
            g2.setStroke(new BasicStroke(stroke));
            g2.setColor(style.getBorderColor());
            g2.draw(shape);
        }

        g2.dispose();
    }
}
