/*
 * Click nbfs:
 * Click nbfs:
 */
package com.mycompany.cs318_finalproject_buynevercry;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Nisha
 */

public class GUIMain extends javax.swing.JFrame {
    
    
    
    private GUISetting settingWindow;
    private GUIEnvelope envelopeWindow;
    private GUIEnvelopeProgress progressWindow;
    
    private int currentRandomAmount = 0;
    private int savedCount = 0;
    public boolean isCurrentRoundSaved = false;
    
    private String userEmail;
    private GUIEnvelope currentEnvelope;
    
    public void refreshData() {
        loadUserData();
        loadUserCurrency();
    }

    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GUIMain.class.getName());

    /**
     * Creates new form GUIMain
     */
    public GUIMain(String email) {
        
        
        this.userEmail = email;
        
        initAllTables();
        
        initComponents();
        
        updateEnvelopeStats();
        initProgressDB();
        FlatLightLaf.setup();
        updateDashboard();
        
       
        btnCreateGoal1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnShuffle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnArchive.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        getContentPane().setBackground(new Color(255, 255, 255));
        
        Image icon = new ImageIcon(getClass().getResource("/images/appicon_normal.png")).getImage();
        setIconImage(icon);
        
        
        initProgressDB();
        loadUserProgress();
        
        shuffleMoney();
        loadUserData();
        loadUserCurrency();
        
        
    }
    
    private void loadUserData() {
        if (userEmail == null || userEmail.isEmpty()) {
            return;
        }

        String url = "jdbc:sqlite:buynevercry.db";
        String sql = "SELECT username FROM users WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String username = rs.getString("username");
                
                jLabel5.setText(username);
                jLabel22.setText(username);
            }
        } catch (SQLException e) {
            System.out.println("Error loading user data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void loadUserCurrency() {
        if (userEmail == null || userEmail.isEmpty()) {
            return;
        }

        String url = "jdbc:sqlite:buynevercry.db";
        String sql = "SELECT currency FROM user_settings WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String currencyStr = rs.getString("currency");
                
                if (currencyStr != null && currencyStr.contains("(") && currencyStr.contains(")")) {
                    String symbol = currencyStr.substring(currencyStr.indexOf("(") + 1, currencyStr.indexOf(")"));
                    
                    jLabel20.setText(symbol);
                    jLabel32.setText(symbol);
                    investlabel1.setText(symbol);
                    jLabel38.setText(symbol);
                    jLabel40.setText(symbol);
                    jLabel66.setText(symbol); 
                }
            } else {
                String defaultSymbol = "$";
                jLabel20.setText(defaultSymbol);
                jLabel32.setText(defaultSymbol);
                investlabel1.setText(defaultSymbol);
                jLabel38.setText(defaultSymbol);
                jLabel40.setText(defaultSymbol);
                jLabel66.setText(defaultSymbol); 
            }
        } catch (SQLException e) {
            System.out.println("Error loading currency: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void shuffleMoney() {
        String url = "jdbc:sqlite:buynevercry.db";
        
        // 1. เช็คเลขซ้ำ (เหมือนเดิม)
        java.util.HashSet<Integer> usedNumbers = new java.util.HashSet<>();
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("SELECT envelope_number FROM active_envelopes WHERE email = ?")) {
            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                usedNumbers.add(rs.getInt("envelope_number"));
            }
        } catch (SQLException e) {}

        if (usedNumbers.size() >= 100) {
            JOptionPane.showMessageDialog(this, "All envelopes opened!");
            return; 
        }

        // 2. สุ่มเลขใหม่
        int newRandom;
        do {
            newRandom = (int) (Math.random() * 100) + 1;
        } while (usedNumbers.contains(newRandom));

        this.currentRandomAmount = newRandom;
        this.isCurrentRoundSaved = false; 
        
        System.out.println("Shuffled (Pending): " + this.currentRandomAmount);
        
        // 3. บันทึกสถานะ "ชั่วคราว" ลง user_progress (เพื่อให้เปิดซองแล้วเจอเลขนี้)
        // แต่ ***ยังไม่เพิ่ม*** ลง active_envelopes
        updateProgressToDB(this.currentRandomAmount);

        // 4. อัปเดตหน้าซอง (ให้เห็นเลข)
        if (envelopeWindow != null && envelopeWindow.isDisplayable()) {
            envelopeWindow.updateDisplay();
        }
        
        // หมายเหตุ: ไม่ต้องเรียก updateEnvelopeStats() ตรงนี้ 
        // เพราะ Stats ต้องไม่เปลี่ยนจนกว่าจะกด Save
    }
    public int getCurrentRandomAmount() {
        return currentRandomAmount;
    }
    public int getSavedCount() {
        return savedCount;
    }
    public void markAsSaved() {
        if (!isCurrentRoundSaved) {
            this.savedCount++;
            this.isCurrentRoundSaved = true;
            
            // 1. บันทึกเลขนี้ลง active_envelopes (ยืนยันการเก็บเงิน)
            String url = "jdbc:sqlite:buynevercry.db";
            String sqlInsert = "INSERT OR IGNORE INTO active_envelopes (email, envelope_number) VALUES (?, ?)";
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                
                pstmt.setString(1, userEmail);
                pstmt.setInt(2, this.currentRandomAmount); // เอาเลขที่สุ่มได้มาบันทึก
                pstmt.executeUpdate();
                
            } catch (SQLException e) {
                System.out.println("Save Active Error: " + e.getMessage());
            }

            // 2. เคลียร์สถานะชั่วคราวเป็น 0 (จบเทิร์น)
            updateProgressToDB(0); 
            this.currentRandomAmount = 0; 
            
            jLabel55.setText(savedCount + "/100 Envelopes");
            
            // 3. *** สั่งอัปเดต Dashboard ทันที ***
            // ค่า moneysavelabel1, jLabel47, 48, 49 จะเปลี่ยนตอนนี้แหละ
            updateEnvelopeStats(); 
            
            // 4. อัปเดตหน้าตารางซอง (ให้เป็นสีฟ้า)
            if (progressWindow != null && progressWindow.isDisplayable()) {
                progressWindow.loadAndShowData();
            }
        }
    }
    private void initProgressDB() {
        String url = "jdbc:sqlite:buynevercry.db";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            

            String createProgressTable = "CREATE TABLE IF NOT EXISTS user_progress (" +
                                         "email TEXT PRIMARY KEY, " +
                                         "saved_count INTEGER DEFAULT 0, " +
                                         "current_random_amount INTEGER DEFAULT 0" + 
                                         ");";
            stmt.execute(createProgressTable);

            try {
                stmt.execute("ALTER TABLE user_progress ADD COLUMN current_random_amount INTEGER DEFAULT 0;");
            } catch (SQLException e) {
            }

            String createArchiveTable = "CREATE TABLE IF NOT EXISTS user_archive (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "email TEXT, " +
                                        "total_saved_count INTEGER, " +
                                        "archived_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                        ");";
            stmt.execute(createArchiveTable);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void loadUserProgress() {
        String url = "jdbc:sqlite:buynevercry.db";
        String sql = "SELECT saved_count, current_random_amount FROM user_progress WHERE email = ?";
        
        boolean needShuffle = false;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                this.savedCount = rs.getInt("saved_count");
                int dbRandom = rs.getInt("current_random_amount");
                
                if (dbRandom > 0) {
                    this.currentRandomAmount = dbRandom;
                } else {
                    needShuffle = true; 
                }
            } else {
                needShuffle = true;
            }
            
            jLabel55.setText(this.savedCount + "/100 Envelopes");
            
        } catch (SQLException e) {
            System.out.println("Load Error: " + e.getMessage());
        }
        
        if (needShuffle) {
            shuffleMoney();
        }
    }
    private void updateProgressToDB(int amountToSave) {
        String url = "jdbc:sqlite:buynevercry.db";
        String sql = "INSERT OR REPLACE INTO user_progress (email, saved_count, current_random_amount) VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail);
            pstmt.setInt(2, this.savedCount);
            pstmt.setInt(3,amountToSave);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("Save Error: " + e.getMessage());
        }
    }
    private void archiveGoal() {
        if (this.savedCount == 0) {
            JOptionPane.showMessageDialog(this, "No progress to archive yet!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to do this? Archiving this challenge will reset all progress. This action cannot be undone.", 
                "Confirm Archive", 
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String url = "jdbc:sqlite:buynevercry.db";
            
            try (Connection conn = DriverManager.getConnection(url)) {
                String deleteActiveSQL = "DELETE FROM active_envelopes WHERE email = ?";
                try (PreparedStatement pstmtDel = conn.prepareStatement(deleteActiveSQL)) {
                    pstmtDel.setString(1, userEmail);
                    pstmtDel.executeUpdate();
                }

                this.savedCount = 0;
                this.isCurrentRoundSaved = false; 
                this.currentRandomAmount = 0;
                
                updateProgressToDB(0);
                
                jLabel55.setText("0/100 Envelopes");
                
                if (progressWindow != null && progressWindow.isDisplayable()) {
                    progressWindow.loadAndShowData();
                }

                JOptionPane.showMessageDialog(this, "Archived successfully!");
                
                updateEnvelopeStats();
                shuffleMoney();

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Archive Error: " + e.getMessage());
            }
        }
    }
    private void initAllTables() {
        String url = "jdbc:sqlite:buynevercry.db";
        
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            
            String createSettingsSQL = "CREATE TABLE IF NOT EXISTS user_settings ("
                        + "email TEXT PRIMARY KEY, "
                        + "salary REAL DEFAULT 0, "
                        + "hours_per_day INTEGER DEFAULT 8, "
                        + "days_per_week INTEGER DEFAULT 5, "
                        + "investment_return REAL DEFAULT 5.0, "
                        + "currency TEXT DEFAULT 'THB (฿)', "
                        + "custom_label TEXT DEFAULT ''"
                        + ");";
            stmt.execute(createSettingsSQL);

            String createProgressSQL = "CREATE TABLE IF NOT EXISTS user_progress (" +
                                         "email TEXT PRIMARY KEY, " +
                                         "saved_count INTEGER DEFAULT 0, " +
                                         "current_random_amount INTEGER DEFAULT 0" + 
                                         ");";
            stmt.execute(createProgressSQL);
            
            String createArchiveSQL = "CREATE TABLE IF NOT EXISTS user_archive (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "email TEXT, " +
                                        "total_saved_count INTEGER, " +
                                        "archived_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                        ");";
            stmt.execute(createArchiveSQL);
            
            String createActiveEnvSQL = "CREATE TABLE IF NOT EXISTS active_envelopes (" +
                                        "email TEXT, " +
                                        "envelope_number INTEGER, " +
                                        "PRIMARY KEY (email, envelope_number)" + 
                                        ");";
            stmt.execute(createActiveEnvSQL);

        } catch (SQLException e) {
            System.out.println("Init DB Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void updateDashboard() {
        String url = "jdbc:sqlite:buynevercry.db";
        String sql = "SELECT SUM(price) as total_saved, " +
                     "SUM(work_minutes) as total_time, " +
                     "SUM(invest_amount) as total_invest " +
                     "FROM goal_decisions WHERE email = ? AND decision = 'DONT_BUY'";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                double totalSaved = rs.getDouble("total_saved");
                double totalTimeMin = rs.getDouble("total_time");
                double totalInvest = rs.getDouble("total_invest");
                
                int hrs = (int) totalTimeMin / 60;
                int mins = (int) totalTimeMin % 60;
                String timeStr;
                if (hrs > 0) {
                    timeStr = String.format("%d hr %d min", hrs, mins);
                } else {
                    timeStr = String.format("%d min", mins);
                }

                moneysavelabel.setText(String.format("%.2f", totalSaved));
//                test.setText(String.format("%.2f", totalSaved));
                timesavedlabel.setText(timeStr);
                investlabel.setText(String.format("%.2f", totalInvest));
            }
            
        } catch (SQLException e) {
            System.out.println("Dashboard Error: " + e.getMessage());
        }
    }
    // ฟังก์ชันอัปเดตสถานะ Envelope Challenge
    public void updateEnvelopeStats() {
        String url = "jdbc:sqlite:buynevercry.db";
        final double TARGET_AMOUNT = 5050.0; // เป้าหมาย 5050
        final int TOTAL_ENVELOPES = 100;

        double totalSaved = 0;
        int openCount = 0;
        double salary = 0;
        double workDays = 0;
        double workHours = 0;

        try (Connection conn = DriverManager.getConnection(url)) {
            
            // 1. ดึงข้อมูลยอดเงินรวม (Sum) และจำนวนซอง (Count)
            String sqlEnv = "SELECT SUM(envelope_number) as total_val, COUNT(*) as count_val FROM active_envelopes WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlEnv)) {
                pstmt.setString(1, userEmail);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    totalSaved = rs.getDouble("total_val");
                    openCount = rs.getInt("count_val");
                }
            }

            // 2. ดึงข้อมูลเงินเดือน
            String sqlSalary = "SELECT salary, days_per_week, hours_per_day FROM user_settings WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlSalary)) {
                pstmt.setString(1, userEmail);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    salary = rs.getDouble("salary");
                    workDays = rs.getDouble("days_per_week");
                    workHours = rs.getDouble("hours_per_day");
                }
            }

            // --- คำนวณและแสดงผล ---

            // A. Moneysavelabel1 (ยอดที่เก็บได้จริง)
            moneysavelabel1.setText(String.format("%.0f", totalSaved));

            // B. jLabel48 (ขาดอีกเท่าไหร่ถึง 5050)
            double remainingMoney = TARGET_AMOUNT - totalSaved;
            if (remainingMoney < 0) remainingMoney = 0;
            jLabel48.setText(String.format("%.0f", remainingMoney));

            // C. jLabel49 (เหลืออีกกี่ซอง)
            int remainingEnvelopes = TOTAL_ENVELOPES - openCount;
            jLabel49.setText(String.valueOf(remainingEnvelopes));

            // D. jLabel47 (ประหยัดเวลาทำงานไปกี่นาที)
            double moneyPerMinute = 0;
            double totalMinutesWork = 52 * workDays * workHours * 60;
            if (totalMinutesWork > 0) {
                moneyPerMinute = salary / totalMinutesWork; 
            }

            double timeSavedMinutes = 0;
            if (moneyPerMinute > 0) {
                timeSavedMinutes = totalSaved / moneyPerMinute;
            }

            // แสดงผลเวลา (hr/min)
            int hrs = (int) timeSavedMinutes / 60;
            int mins = (int) timeSavedMinutes % 60;
            if (hrs > 0) {
                jLabel47.setText(String.format("%d hr %d min", hrs, mins));
            } else {
                jLabel47.setText(String.format("%d min", mins));
            }

            // E. ProgressBar & Percentage (ส่วนที่แก้ไข Logic)
            // คำนวณ % จากยอดเงินเทียบกับ 5050
            double progressPercentage = (totalSaved / TARGET_AMOUNT) * 100;
            
            // จำกัดไม่ให้เกิน 100% (เผื่อ error)
            if (progressPercentage > 100) progressPercentage = 100;
            
            int progressInt = (int) progressPercentage;

            jProgressBar1.setMaximum(100);
            jProgressBar1.setValue(progressInt);
            
            // แสดงตัวเลขเปอร์เซ็นต์
            jLabel46.setText(progressInt + "%");
            jLabel17.setText("You're "+progressInt + "% there! Keep going!");

        } catch (SQLException e) {
            System.out.println("Stats Error: " + e.getMessage());
        }
    }


    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel27 = new javax.swing.JLabel();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jPopupMenu1 = new javax.swing.JPopupMenu();
        jPopupMenu2 = new javax.swing.JPopupMenu();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        roundedPanel1 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        roundedPanel2 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        btnSetting = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        btnContactSupport = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        roundedPanel3 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel13 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        roundedPanel4 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        roundedPanel5 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        moneysavelabel = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        timesavedlabel = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        investlabel = new javax.swing.JLabel();
        investlabel1 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        roundedPanel6 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel26 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        roundedPanel8 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel38 = new javax.swing.JLabel();
        moneysavelabel2 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel40 = new javax.swing.JLabel();
        moneysavelabel1 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel43 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel46 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        roundedPanel9 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel51 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel66 = new javax.swing.JLabel();
        btnEnvelope = new javax.swing.JLabel();
        btnArchive = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        btnShuffle = new javax.swing.JPanel();
        jLabel53 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        roundedPanel7 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        btnCreateGoal = new javax.swing.JPanel();
        jLabel57 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        btnEnvelopeProgress = new javax.swing.JPanel();
        jLabel62 = new javax.swing.JLabel();
        jLabel63 = new javax.swing.JLabel();
        roundedPanel10 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        btnCreateGoal1 = new javax.swing.JPanel();
        jLabel58 = new javax.swing.JLabel();
        jLabel59 = new javax.swing.JLabel();

        jLabel27.setText("jLabel27");

        jMenu1.setText("jMenu1");

        jMenu2.setText("jMenu2");

        jMenu3.setText("jMenu3");

        jMenuItem1.setText("jMenuItem1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Buynevercry");
        setBackground(new java.awt.Color(255, 255, 255));
        setSize(new java.awt.Dimension(1440, 1024));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(222, 975));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/appicon_small.png"))); // NOI18N

        jLabel2.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 20)); // NOI18N
        jLabel2.setText("Buynevercry");

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/homepage.png"))); // NOI18N

        jSeparator1.setForeground(new java.awt.Color(213, 213, 213));

        roundedPanel1.setCornerRadius(8);
        roundedPanel1.setPreferredSize(new java.awt.Dimension(222, 63));

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/avatar.png"))); // NOI18N

        jLabel5.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 16)); // NOI18N
        jLabel5.setText("{Username}");

        jLabel6.setFont(new java.awt.Font("Inter", 0, 12)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(126, 126, 126));
        jLabel6.setText("Personal Account");

        javax.swing.GroupLayout roundedPanel1Layout = new javax.swing.GroupLayout(roundedPanel1);
        roundedPanel1.setLayout(roundedPanel1Layout);
        roundedPanel1Layout.setHorizontalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jLabel4)
                .addGap(16, 16, 16)
                .addGroup(roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE))
                .addContainerGap())
        );
        roundedPanel1Layout.setVerticalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel1Layout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(jLabel6)
                .addGap(14, 14, 14))
        );

        roundedPanel2.setCornerRadius(8);
        roundedPanel2.setPreferredSize(new java.awt.Dimension(222, 37));

        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/house.png"))); // NOI18N

        jLabel8.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 16)); // NOI18N
        jLabel8.setText("Dashboard");

        javax.swing.GroupLayout roundedPanel2Layout = new javax.swing.GroupLayout(roundedPanel2);
        roundedPanel2.setLayout(roundedPanel2Layout);
        roundedPanel2Layout.setHorizontalGroup(
            roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel2Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel7)
                .addGap(9, 9, 9)
                .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                .addContainerGap())
        );
        roundedPanel2Layout.setVerticalGroup(
            roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
            .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        btnSetting.setBackground(new java.awt.Color(255, 255, 255));
        btnSetting.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnSetting.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSettingMouseClicked(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Inter 18pt Medium", 0, 16)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(126, 126, 126));
        jLabel10.setText("Setting");

        jLabel9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-setting-32_1.png"))); // NOI18N

        javax.swing.GroupLayout btnSettingLayout = new javax.swing.GroupLayout(btnSetting);
        btnSetting.setLayout(btnSettingLayout);
        btnSettingLayout.setHorizontalGroup(
            btnSettingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnSettingLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLabel9)
                .addGap(10, 10, 10)
                .addComponent(jLabel10)
                .addContainerGap())
        );
        btnSettingLayout.setVerticalGroup(
            btnSettingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnSettingLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(btnSettingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addContainerGap())
        );

        btnContactSupport.setBackground(new java.awt.Color(255, 255, 255));
        btnContactSupport.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnContactSupport.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnContactSupportMouseClicked(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Inter 18pt Medium", 0, 16)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(126, 126, 126));
        jLabel11.setText("Logout");
        jLabel11.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel11MouseClicked(evt);
            }
        });

        jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/logout-04.png"))); // NOI18N

        javax.swing.GroupLayout btnContactSupportLayout = new javax.swing.GroupLayout(btnContactSupport);
        btnContactSupport.setLayout(btnContactSupportLayout);
        btnContactSupportLayout.setHorizontalGroup(
            btnContactSupportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnContactSupportLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel12)
                .addGap(10, 10, 10)
                .addComponent(jLabel11)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        btnContactSupportLayout.setVerticalGroup(
            btnContactSupportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, btnContactSupportLayout.createSequentialGroup()
                .addContainerGap(7, Short.MAX_VALUE)
                .addGroup(btnContactSupportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel12)
                    .addComponent(jLabel11))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(10, 10, 10)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel3))
            .addComponent(jSeparator1)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(roundedPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(roundedPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnContactSupport, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSetting, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(29, 29, 29)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(29, 29, 29)
                .addComponent(roundedPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(29, 29, 29)
                .addComponent(roundedPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19)
                .addComponent(btnSetting, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 659, Short.MAX_VALUE)
                .addComponent(btnContactSupport, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        roundedPanel3.setToolTipText("");
        roundedPanel3.setBottomLeft(0);
        roundedPanel3.setBottomRight(0);

        jLabel13.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 22)); // NOI18N
        jLabel13.setText("Dashboard");

        jSeparator2.setBackground(new java.awt.Color(213, 213, 213));
        jSeparator2.setForeground(new java.awt.Color(213, 213, 213));

        roundedPanel4.setBorderColor(new java.awt.Color(59, 118, 228));
        roundedPanel4.setOpacityPercent(13);
        roundedPanel4.setPanelColor(new java.awt.Color(59, 118, 228));
        roundedPanel4.setPreferredSize(new java.awt.Dimension(1113, 63));

        jLabel16.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 16)); // NOI18N
        jLabel16.setText("Hey");

        jLabel17.setFont(new java.awt.Font("Inter 18pt", 0, 14)); // NOI18N
        jLabel17.setText("You're 60% there! Keep going!");

        jLabel18.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/waving-hand-sign_1f44b.png"))); // NOI18N

        jLabel22.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 16)); // NOI18N
        jLabel22.setText("{Username}");

        jLabel23.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 16)); // NOI18N
        jLabel23.setText(",");

        jLabel61.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/banklocker.png"))); // NOI18N

        javax.swing.GroupLayout roundedPanel4Layout = new javax.swing.GroupLayout(roundedPanel4);
        roundedPanel4.setLayout(roundedPanel4Layout);
        roundedPanel4Layout.setHorizontalGroup(
            roundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel4Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(roundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 740, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(roundedPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel22)
                        .addGap(2, 2, 2)
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel18)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 154, Short.MAX_VALUE)
                .addComponent(jLabel61)
                .addGap(162, 162, 162))
        );
        roundedPanel4Layout.setVerticalGroup(
            roundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel4Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(roundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel18)
                    .addGroup(roundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel16)
                        .addComponent(jLabel22)
                        .addComponent(jLabel23)))
                .addGap(0, 0, 0)
                .addComponent(jLabel17)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(roundedPanel4Layout.createSequentialGroup()
                .addComponent(jLabel61)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jLabel14.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 16)); // NOI18N
        jLabel14.setText("Accounts");

        roundedPanel5.setMaximumSize(null);
        roundedPanel5.setPreferredSize(new java.awt.Dimension(1113, 63));

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));

        jLabel21.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-info-50.png"))); // NOI18N

        jLabel19.setFont(new java.awt.Font("Inter 18pt Medium", 0, 13)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(126, 126, 126));
        jLabel19.setText("Money Saved");

        jLabel20.setFont(new java.awt.Font("Inter 18pt Medium", 0, 20)); // NOI18N
        jLabel20.setText("{Currency}");

        moneysavelabel.setFont(new java.awt.Font("Inter 18pt Medium", 0, 20)); // NOI18N
        moneysavelabel.setText("{Money_amt}");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(moneysavelabel))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel21)))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel21)
                    .addComponent(jLabel19))
                .addGap(0, 0, 0)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(moneysavelabel))
                .addContainerGap())
        );

        jPanel7.setBackground(new java.awt.Color(255, 255, 255));

        jLabel30.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-info-50.png"))); // NOI18N

        jLabel31.setFont(new java.awt.Font("Inter 18pt Medium", 0, 13)); // NOI18N
        jLabel31.setForeground(new java.awt.Color(126, 126, 126));
        jLabel31.setText("Work Time Saved");

        jLabel32.setFont(new java.awt.Font("Inter 18pt Medium", 0, 20)); // NOI18N
        jLabel32.setText("{Currency}");

        timesavedlabel.setFont(new java.awt.Font("Inter 18pt Medium", 0, 20)); // NOI18N
        timesavedlabel.setText("{Money_amt}");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel32)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(timesavedlabel))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel31)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel30)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel30)
                    .addComponent(jLabel31))
                .addGap(0, 0, 0)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel32)
                    .addComponent(timesavedlabel))
                .addContainerGap())
        );

        jPanel8.setBackground(new java.awt.Color(255, 255, 255));

        jLabel34.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-info-50.png"))); // NOI18N

        jLabel35.setFont(new java.awt.Font("Inter 18pt Medium", 0, 13)); // NOI18N
        jLabel35.setForeground(new java.awt.Color(126, 126, 126));
        jLabel35.setText("Your Investments could grow to");

        investlabel.setFont(new java.awt.Font("Inter 18pt Medium", 0, 20)); // NOI18N
        investlabel.setText("{TimeSaved}");

        investlabel1.setFont(new java.awt.Font("Inter 18pt Medium", 0, 20)); // NOI18N
        investlabel1.setText("{Currency}");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addComponent(investlabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(investlabel))
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addComponent(jLabel35)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel34)))
                .addContainerGap(121, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel34)
                    .addComponent(jLabel35))
                .addGap(0, 0, 0)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(investlabel)
                    .addComponent(investlabel1))
                .addContainerGap())
        );

        jLabel25.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/coin.png"))); // NOI18N

        javax.swing.GroupLayout roundedPanel5Layout = new javax.swing.GroupLayout(roundedPanel5);
        roundedPanel5.setLayout(roundedPanel5Layout);
        roundedPanel5Layout.setHorizontalGroup(
            roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel5Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(118, 118, 118)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(118, 118, 118)
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel25)
                .addGap(14, 14, 14))
        );
        roundedPanel5Layout.setVerticalGroup(
            roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel5Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addGroup(roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel5Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        roundedPanel6.setPreferredSize(new java.awt.Dimension(1113, 415));

        jLabel26.setFont(new java.awt.Font("Inter 18pt Medium", 0, 16)); // NOI18N
        jLabel26.setText("Saving challenge");

        jLabel28.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-info-50.png"))); // NOI18N

        roundedPanel8.setPreferredSize(new java.awt.Dimension(350, 325));

        jLabel38.setFont(new java.awt.Font("Inter 18pt Medium", 0, 18)); // NOI18N
        jLabel38.setText("$");

        moneysavelabel2.setFont(new java.awt.Font("Inter 18pt Medium", 0, 18)); // NOI18N
        moneysavelabel2.setText("5,050");

        jProgressBar1.setBackground(new java.awt.Color(236, 234, 234));
        jProgressBar1.setForeground(new java.awt.Color(59, 118, 228));
        jProgressBar1.setPreferredSize(new java.awt.Dimension(146, 5));
        jProgressBar1.setString("");
        jProgressBar1.setStringPainted(true);

        jLabel40.setFont(new java.awt.Font("Inter 18pt Medium", 0, 18)); // NOI18N
        jLabel40.setText("{Currency}");

        moneysavelabel1.setFont(new java.awt.Font("Inter 18pt Medium", 0, 18)); // NOI18N
        moneysavelabel1.setText("{Monney_amt}");

        jLabel42.setFont(new java.awt.Font("Inter 18pt Medium", 0, 14)); // NOI18N
        jLabel42.setForeground(new java.awt.Color(126, 126, 126));
        jLabel42.setText("saved so far");

        jSeparator3.setBackground(new java.awt.Color(213, 213, 213));
        jSeparator3.setForeground(new java.awt.Color(213, 213, 213));

        jLabel43.setFont(new java.awt.Font("Inter 18pt Medium", 0, 14)); // NOI18N
        jLabel43.setForeground(new java.awt.Color(126, 126, 126));
        jLabel43.setText("Work time saved");

        jLabel44.setFont(new java.awt.Font("Inter 18pt Medium", 0, 14)); // NOI18N
        jLabel44.setForeground(new java.awt.Color(126, 126, 126));
        jLabel44.setText("Remaining");

        jLabel45.setFont(new java.awt.Font("Inter 18pt Medium", 0, 14)); // NOI18N
        jLabel45.setForeground(new java.awt.Color(126, 126, 126));
        jLabel45.setText("Remaining challenge");

        jSeparator4.setBackground(new java.awt.Color(213, 213, 213));
        jSeparator4.setForeground(new java.awt.Color(213, 213, 213));

        jLabel46.setFont(new java.awt.Font("Inter 18pt Medium", 0, 14)); // NOI18N
        jLabel46.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel46.setText("{Percentage}");

        jLabel47.setFont(new java.awt.Font("Inter 18pt Medium", 0, 14)); // NOI18N
        jLabel47.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel47.setText("3 hours, 11 minutes");

        jLabel48.setFont(new java.awt.Font("Inter 18pt Medium", 0, 14)); // NOI18N
        jLabel48.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel48.setText("4440");

        jLabel49.setFont(new java.awt.Font("Inter 18pt Medium", 0, 14)); // NOI18N
        jLabel49.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel49.setText("17");

        roundedPanel9.setCornerRadius(8);
        roundedPanel9.setPreferredSize(new java.awt.Dimension(37, 37));

        jLabel51.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel51.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/money-bags-23.png"))); // NOI18N

        javax.swing.GroupLayout roundedPanel9Layout = new javax.swing.GroupLayout(roundedPanel9);
        roundedPanel9.setLayout(roundedPanel9Layout);
        roundedPanel9Layout.setHorizontalGroup(
            roundedPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel51, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );
        roundedPanel9Layout.setVerticalGroup(
            roundedPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel51, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        jLabel50.setFont(new java.awt.Font("Inter 18pt Medium", 0, 18)); // NOI18N
        jLabel50.setText("Money saved");

        jLabel66.setFont(new java.awt.Font("Inter 18pt Medium", 0, 14)); // NOI18N
        jLabel66.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel66.setText("$");

        javax.swing.GroupLayout roundedPanel8Layout = new javax.swing.GroupLayout(roundedPanel8);
        roundedPanel8.setLayout(roundedPanel8Layout);
        roundedPanel8Layout.setHorizontalGroup(
            roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator4)
            .addGroup(roundedPanel8Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(roundedPanel8Layout.createSequentialGroup()
                        .addComponent(roundedPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(jLabel50)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(roundedPanel8Layout.createSequentialGroup()
                        .addComponent(jLabel38)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(moneysavelabel2)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel8Layout.createSequentialGroup()
                        .addGroup(roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(roundedPanel8Layout.createSequentialGroup()
                                .addComponent(jLabel44)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel48)
                                .addGap(3, 3, 3)
                                .addComponent(jLabel66))
                            .addGroup(roundedPanel8Layout.createSequentialGroup()
                                .addComponent(jLabel45)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel49))
                            .addGroup(roundedPanel8Layout.createSequentialGroup()
                                .addComponent(jLabel43)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel47))
                            .addGroup(roundedPanel8Layout.createSequentialGroup()
                                .addComponent(jLabel40)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(moneysavelabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel42)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 643, Short.MAX_VALUE)
                                .addComponent(jLabel46, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(14, 14, 14))))
        );
        roundedPanel8Layout.setVerticalGroup(
            roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel8Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(roundedPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel50, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(23, 23, 23)
                .addGroup(roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel38)
                    .addComponent(moneysavelabel2))
                .addGap(8, 8, 8)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel40)
                    .addComponent(moneysavelabel1)
                    .addComponent(jLabel42)
                    .addComponent(jLabel46))
                .addGap(24, 24, 24)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel43)
                    .addComponent(jLabel47))
                .addGap(13, 13, 13)
                .addGroup(roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel44)
                    .addComponent(jLabel48)
                    .addComponent(jLabel66))
                .addGap(13, 13, 13)
                .addGroup(roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(jLabel49))
                .addGap(22, 22, 22)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout roundedPanel6Layout = new javax.swing.GroupLayout(roundedPanel6);
        roundedPanel6.setLayout(roundedPanel6Layout);
        roundedPanel6Layout.setHorizontalGroup(
            roundedPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel6Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel26)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel28)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(roundedPanel6Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(roundedPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, 1085, Short.MAX_VALUE)
                .addGap(14, 14, 14))
        );
        roundedPanel6Layout.setVerticalGroup(
            roundedPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel6Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(roundedPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(31, 31, 31)
                .addComponent(roundedPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22))
        );

        btnEnvelope.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        btnEnvelope.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/111.png"))); // NOI18N
        btnEnvelope.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnEnvelope.setPreferredSize(new java.awt.Dimension(100, 226));
        btnEnvelope.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnEnvelopeMouseClicked(evt);
            }
        });

        btnArchive.setBackground(new java.awt.Color(255, 255, 255));

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/archive_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.png"))); // NOI18N
        jLabel15.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel15MouseClicked(evt);
            }
        });

        jLabel24.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 14)); // NOI18N
        jLabel24.setText("Archive");

        javax.swing.GroupLayout btnArchiveLayout = new javax.swing.GroupLayout(btnArchive);
        btnArchive.setLayout(btnArchiveLayout);
        btnArchiveLayout.setHorizontalGroup(
            btnArchiveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnArchiveLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(btnArchiveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        btnArchiveLayout.setVerticalGroup(
            btnArchiveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnArchiveLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel24)
                .addContainerGap())
        );

        btnShuffle.setBackground(new java.awt.Color(255, 255, 255));

        jLabel53.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel53.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-shuffle-64.png"))); // NOI18N
        jLabel53.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel53MouseClicked(evt);
            }
        });

        jLabel54.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 14)); // NOI18N
        jLabel54.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel54.setText("Shuffle");

        javax.swing.GroupLayout btnShuffleLayout = new javax.swing.GroupLayout(btnShuffle);
        btnShuffle.setLayout(btnShuffleLayout);
        btnShuffleLayout.setHorizontalGroup(
            btnShuffleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnShuffleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(btnShuffleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel54, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel53, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        btnShuffleLayout.setVerticalGroup(
            btnShuffleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnShuffleLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel53)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel54)
                .addContainerGap())
        );

        jLabel55.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 14)); // NOI18N
        jLabel55.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel55.setText("1/100 Envelopes");

        jLabel56.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/fire_1f525.png"))); // NOI18N

        roundedPanel7.setCornerRadius(8);
        roundedPanel7.setPanelColor(new java.awt.Color(0, 0, 0));
        roundedPanel7.setPreferredSize(new java.awt.Dimension(149, 37));

        btnCreateGoal.setBackground(new java.awt.Color(0, 0, 0));
        btnCreateGoal.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnCreateGoalMouseClicked(evt);
            }
        });

        jLabel57.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel57.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/white-icons8-plus-24.png"))); // NOI18N
        jLabel57.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        jLabel52.setFont(new java.awt.Font("Inter 18pt", 0, 14)); // NOI18N
        jLabel52.setForeground(new java.awt.Color(255, 255, 255));
        jLabel52.setText("Create Goal");
        jLabel52.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout btnCreateGoalLayout = new javax.swing.GroupLayout(btnCreateGoal);
        btnCreateGoal.setLayout(btnCreateGoalLayout);
        btnCreateGoalLayout.setHorizontalGroup(
            btnCreateGoalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnCreateGoalLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel57, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel52, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        btnCreateGoalLayout.setVerticalGroup(
            btnCreateGoalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnCreateGoalLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(btnCreateGoalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel52, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel57, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout roundedPanel7Layout = new javax.swing.GroupLayout(roundedPanel7);
        roundedPanel7.setLayout(roundedPanel7Layout);
        roundedPanel7Layout.setHorizontalGroup(
            roundedPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel7Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(btnCreateGoal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );
        roundedPanel7Layout.setVerticalGroup(
            roundedPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel7Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(btnCreateGoal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        btnEnvelopeProgress.setBackground(new java.awt.Color(255, 255, 255));
        btnEnvelopeProgress.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnEnvelopeProgress.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnEnvelopeProgressMouseClicked(evt);
            }
        });

        jLabel62.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel62.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/clock_black.png"))); // NOI18N

        jLabel63.setFont(new java.awt.Font("Inter 18pt SemiBold", 0, 14)); // NOI18N
        jLabel63.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel63.setText("Progress");

        javax.swing.GroupLayout btnEnvelopeProgressLayout = new javax.swing.GroupLayout(btnEnvelopeProgress);
        btnEnvelopeProgress.setLayout(btnEnvelopeProgressLayout);
        btnEnvelopeProgressLayout.setHorizontalGroup(
            btnEnvelopeProgressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnEnvelopeProgressLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(btnEnvelopeProgressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel63, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel62, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        btnEnvelopeProgressLayout.setVerticalGroup(
            btnEnvelopeProgressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnEnvelopeProgressLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel62)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel63)
                .addContainerGap())
        );

        roundedPanel10.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel10.setCornerRadius(8);
        roundedPanel10.setPreferredSize(new java.awt.Dimension(149, 37));

        btnCreateGoal1.setBackground(new java.awt.Color(255, 255, 255));
        btnCreateGoal1.setForeground(new java.awt.Color(255, 255, 255));
        btnCreateGoal1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnCreateGoal1MouseClicked(evt);
            }
        });

        jLabel58.setForeground(new java.awt.Color(255, 255, 255));
        jLabel58.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel58.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/clock_black.png"))); // NOI18N
        jLabel58.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        jLabel59.setFont(new java.awt.Font("Inter 18pt", 0, 14)); // NOI18N
        jLabel59.setText("History");
        jLabel59.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout btnCreateGoal1Layout = new javax.swing.GroupLayout(btnCreateGoal1);
        btnCreateGoal1.setLayout(btnCreateGoal1Layout);
        btnCreateGoal1Layout.setHorizontalGroup(
            btnCreateGoal1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnCreateGoal1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel58, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel59, javax.swing.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE))
        );
        btnCreateGoal1Layout.setVerticalGroup(
            btnCreateGoal1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnCreateGoal1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(btnCreateGoal1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel59, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel58, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout roundedPanel10Layout = new javax.swing.GroupLayout(roundedPanel10);
        roundedPanel10.setLayout(roundedPanel10Layout);
        roundedPanel10Layout.setHorizontalGroup(
            roundedPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnCreateGoal1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );
        roundedPanel10Layout.setVerticalGroup(
            roundedPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel10Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(btnCreateGoal1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout roundedPanel3Layout = new javax.swing.GroupLayout(roundedPanel3);
        roundedPanel3.setLayout(roundedPanel3Layout);
        roundedPanel3Layout.setHorizontalGroup(
            roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator2)
            .addGroup(roundedPanel3Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(roundedPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addGap(790, 790, 790)
                        .addComponent(roundedPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(roundedPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(roundedPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(roundedPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel13)
                        .addComponent(roundedPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(18, Short.MAX_VALUE))
            .addGroup(roundedPanel3Layout.createSequentialGroup()
                .addGap(57, 57, 57)
                .addComponent(btnArchive, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(190, 190, 190)
                .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel55, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel56)
                    .addComponent(btnEnvelope, javax.swing.GroupLayout.PREFERRED_SIZE, 343, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnShuffle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEnvelopeProgress, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(55, 55, 55))
        );
        roundedPanel3Layout.setVerticalGroup(
            roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel3Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel13)
                .addGap(17, 17, 17)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addComponent(roundedPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23)
                .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(roundedPanel3Layout.createSequentialGroup()
                        .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(roundedPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(roundedPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(17, 17, 17)
                        .addComponent(roundedPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(roundedPanel3Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(btnEnvelopeProgress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(btnShuffle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnArchive, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(52, 52, 52))
                            .addGroup(roundedPanel3Layout.createSequentialGroup()
                                .addComponent(btnEnvelope, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel56)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel55)
                                .addContainerGap(39, Short.MAX_VALUE))))
                    .addGroup(roundedPanel3Layout.createSequentialGroup()
                        .addComponent(roundedPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(roundedPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 977, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(51, Short.MAX_VALUE))
                    .addComponent(roundedPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnSettingMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSettingMouseClicked
        
        if (settingWindow == null || !settingWindow.isDisplayable()) {
            settingWindow = new GUISetting(userEmail,this);
            settingWindow.setVisible(true);
        } else {
            settingWindow.setVisible(true);
            settingWindow.toFront();
            settingWindow.requestFocus();
        }
    }//GEN-LAST:event_btnSettingMouseClicked

    private void btnEnvelopeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEnvelopeMouseClicked
        
        if (this.currentRandomAmount == 0) {
            shuffleMoney();
        }

        if (envelopeWindow == null || !envelopeWindow.isDisplayable()) {
            envelopeWindow = new GUIEnvelope(this.userEmail, this); 
            envelopeWindow.setVisible(true);
        } else {
            envelopeWindow.setVisible(true);
            envelopeWindow.toFront();
            envelopeWindow.updateDisplay(); 
        }
    }//GEN-LAST:event_btnEnvelopeMouseClicked

    private void btnContactSupportMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnContactSupportMouseClicked
        
    }//GEN-LAST:event_btnContactSupportMouseClicked

    private void btnCreateGoalMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCreateGoalMouseClicked
        
        String input = JOptionPane.showInputDialog(null, "PURCHASE :");
        if (input != null && !input.trim().isEmpty()) {
            
            GUIGoalCreate ggc = new GUIGoalCreate(input, this.userEmail);
            
            ggc.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    updateDashboard(); 
                }
            });
            
            ggc.setVisible(true);
        }
    }//GEN-LAST:event_btnCreateGoalMouseClicked

    private void jLabel11MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel11MouseClicked
        int choice = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to log out?", 
                "Logout", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            this.dispose(); 

            GUILogin loginScreen = new GUILogin();
            loginScreen.setVisible(true);
        }
    }//GEN-LAST:event_jLabel11MouseClicked

    private void btnEnvelopeProgressMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEnvelopeProgressMouseClicked

        if (progressWindow == null || !progressWindow.isDisplayable()) {
            progressWindow = new GUIEnvelopeProgress(this, this.userEmail);
            progressWindow.setVisible(true);
        } else {
            progressWindow.setVisible(true);
            progressWindow.toFront();
            progressWindow.loadAndShowData();
        }
        
    }//GEN-LAST:event_btnEnvelopeProgressMouseClicked

    private void btnCreateGoal1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCreateGoal1MouseClicked
        // TODO add your handling code here:
        GUIGoalHistory history = new GUIGoalHistory(this.userEmail);
        history.setVisible(true);
    }//GEN-LAST:event_btnCreateGoal1MouseClicked

    private void jLabel53MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel53MouseClicked
        shuffleMoney();
        JOptionPane.showMessageDialog(this, "New Envelope Generated!");
    }//GEN-LAST:event_jLabel53MouseClicked

    private void jLabel15MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel15MouseClicked
        archiveGoal();
    }//GEN-LAST:event_jLabel15MouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http:
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new GUIMain("test@example.com").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel btnArchive;
    private javax.swing.JPanel btnContactSupport;
    private javax.swing.JPanel btnCreateGoal;
    private javax.swing.JPanel btnCreateGoal1;
    private javax.swing.JLabel btnEnvelope;
    private javax.swing.JPanel btnEnvelopeProgress;
    private javax.swing.JPanel btnSetting;
    private javax.swing.JPanel btnShuffle;
    private javax.swing.JLabel investlabel;
    private javax.swing.JLabel investlabel1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JPopupMenu jPopupMenu2;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JLabel moneysavelabel;
    private javax.swing.JLabel moneysavelabel1;
    private javax.swing.JLabel moneysavelabel2;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel1;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel10;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel2;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel3;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel4;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel5;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel6;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel7;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel8;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel9;
    private javax.swing.JLabel timesavedlabel;
    // End of variables declaration//GEN-END:variables
}
