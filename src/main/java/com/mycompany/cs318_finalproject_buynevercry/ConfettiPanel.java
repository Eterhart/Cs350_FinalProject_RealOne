package com.mycompany.cs318_finalproject_buynevercry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class ConfettiPanel extends JPanel {

    private static class Confetti {
        double x, y;
        double vx, vy;
        int size;
        Color color;
        double drag; // แรงต้านอากาศ (ทำให้ตกช้า/ลอยๆ)
    }

    private final ArrayList<Confetti> confettis = new ArrayList<>();
    private final Random rand = new Random();

    // 🎨 สีตามที่ขอ: #ec1e45 #fce855 #fe7e99 #f6fbf6
    private final Color[] PALETTE = {
            Color.decode("#ec1e45"),
            Color.decode("#fce855"),
            Color.decode("#fe7e99"),
            Color.decode("#f6fbf6")
    };

    // ยิงจาก “ตำแหน่งปุ่ม” (พิกัดใน confettiPanel)
    public void burstFrom(int px, int py) {
        int w = Math.max(getWidth(), 1);
        int h = Math.max(getHeight(), 1);

        // จำนวนชิ้น (ปรับได้)
        int n = 160;
        for (int i = 0; i < n; i++) {
            Confetti c = new Confetti();
            c.x = px;
            c.y = py;

            c.size = rand.nextInt(6) + 4;
            c.color = PALETTE[rand.nextInt(PALETTE.length)];

            // กระจายซ้าย-ขวา
            c.vx = (rand.nextDouble() * 8.0) - 4.0;     // -4..+4
            // พุ่งขึ้นแรงหน่อย (ค่าติดลบ = ขึ้น)
            c.vy = -(rand.nextDouble() * 14.0 + 10.0);  // -10..-24

            // drag ทำให้ “ตกช้า/ลอย” (ยิ่งมากยิ่งช้า)
            c.drag = 0.985 + rand.nextDouble() * 0.01;  // ~0.985..0.995

            confettis.add(c);
        }
        repaint();
    }

    public void updateConfetti() {
        // gravity เบาๆ เพื่อให้ตกช้า
        final double g = 0.20;

        for (int i = confettis.size() - 1; i >= 0; i--) {
            Confetti c = confettis.get(i);

            c.vy += g;           // เริ่มตก
            c.vx *= c.drag;      // ต้านอากาศ
            c.vy *= c.drag;

            c.x += c.vx;
            c.y += c.vy;

            // ลบทิ้งเมื่อหลุดจอ (กัน list โตเรื่อยๆ)
            if (c.y > getHeight() + 60 || c.x < -80 || c.x > getWidth() + 80) {
                confettis.remove(i);
            }
        }
        repaint();
    }

    public boolean isEmpty() {
        return confettis.isEmpty();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        for (Confetti c : confettis) {
            g2.setColor(c.color);
            g2.fillRoundRect((int) c.x, (int) c.y, c.size, c.size, 3, 3);
        }
    }
}
