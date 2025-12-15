/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JInternalFrame.java to edit this template
 */
package com.mycompany.cs318_finalproject_buynevercry;

import java.awt.*;
import javax.swing.*;
import java.sql.*;

/**
 *
 * @author Nisha
 */
public class GUISetting extends javax.swing.JFrame {

    /**
     * Creates new form GUISetting
     */
    private String currentEmail;
    private GUIMain guiMain;
    
    public GUISetting(String email, GUIMain main) {
        this.currentEmail = email;
        this.guiMain = main;
        
        
        initComponents();
        getContentPane().setBackground(new Color(255, 255, 255));
        Image icon = new ImageIcon(getClass().getResource("/images/appicon_normal.png")).getImage();
        setIconImage(icon);
        
        loadUserSettings();
        
        jComboBox3.setBackground(Color.WHITE);
        jComboBox3.setOpaque(true);
        
        initAutoSave();
    }
    public GUISetting() {
        initComponents();
    }
    private void loadUserSettings() {
        if (currentEmail == null || currentEmail.isEmpty()) return;

        String url = "jdbc:sqlite:buynevercry.db";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS user_settings ("
                        + "email TEXT PRIMARY KEY, "
                        + "salary REAL DEFAULT 0, "
                        + "hours_per_day INTEGER DEFAULT 8, "
                        + "days_per_week INTEGER DEFAULT 5, "
                        + "investment_return REAL DEFAULT 5.0, "
                        + "currency TEXT DEFAULT 'THB (฿)', "
                        + "custom_label TEXT DEFAULT ''"
                        + ");";
                
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableSQL);
                }

                String querySettings = "SELECT * FROM user_settings WHERE email = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(querySettings)) {
                    pstmt.setString(1, currentEmail);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        jTextFieldSalary.setText(String.valueOf(rs.getDouble("salary")));
                        jTextFieldHours.setText(String.valueOf(rs.getInt("hours_per_day")));
                        jTextFieldDays.setText(String.valueOf(rs.getInt("days_per_week")));
                        jTextFieldInvest.setText(String.valueOf(rs.getDouble("investment_return")));
                        
                        String savedCurrency = rs.getString("currency");
                        if (savedCurrency != null) {
                            jComboBox3.setSelectedItem(savedCurrency);
                        } else {
                        jComboBox3.setSelectedIndex(0); 
                        }
                        
                        
                        String savedLabel = rs.getString("custom_label");
                        if (savedLabel != null) jTextField2.setText(savedLabel);

                    } else {
                        String queryUserMain = "SELECT yearly_salary FROM users WHERE email = ?";
                        try (PreparedStatement pstmtUser = conn.prepareStatement(queryUserMain)) {
                            pstmtUser.setString(1, currentEmail);
                            ResultSet rsUser = pstmtUser.executeQuery();
                            if (rsUser.next()) {
                                jTextFieldSalary.setText(String.valueOf(rsUser.getDouble("yearly_salary")));
                            } else {
                                jTextFieldSalary.setText("0.0");
                            }
                        }
                        
                        jTextFieldHours.setText("8");
                        jTextFieldDays.setText("5");
                        jTextFieldInvest.setText("5.0");
                        jComboBox3.setSelectedIndex(0);
                        jTextField2.setText("");
                    }
                    updateCurrencyLabel();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void saveSettings() {
        if (currentEmail == null || currentEmail.isEmpty()) return;

        String url = "jdbc:sqlite:buynevercry.db";
        String sql = "INSERT OR REPLACE INTO user_settings "
                   + "(email, salary, hours_per_day, days_per_week, investment_return, currency, custom_label) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            double salary = 0;
            int hours = 8;
            int days = 5;
            double invest = 5.0;

            try {
                if (!jTextFieldSalary.getText().isEmpty()) salary = Double.parseDouble(jTextFieldSalary.getText());
                if (!jTextFieldHours.getText().isEmpty()) hours = Integer.parseInt(jTextFieldHours.getText());
                if (!jTextFieldDays.getText().isEmpty()) days = Integer.parseInt(jTextFieldDays.getText());
                if (!jTextFieldInvest.getText().isEmpty()) invest = Double.parseDouble(jTextFieldInvest.getText());
            } catch (NumberFormatException ex) {
                System.out.println("Invalid format, skip save");
                return;
            }

            String currency = (String) jComboBox3.getSelectedItem();
            String customLabel = jTextField2.getText();

            pstmt.setString(1, currentEmail);
            pstmt.setDouble(2, salary);
            pstmt.setInt(3, hours);
            pstmt.setInt(4, days);
            pstmt.setDouble(5, invest);
            pstmt.setString(6, currency);
            pstmt.setString(7, customLabel);

            pstmt.executeUpdate();
            System.out.println("Auto-saved all settings for: " + currentEmail);
            
            if (guiMain != null) {
                guiMain.refreshData();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void initAutoSave() {
        java.awt.event.FocusAdapter focusLostListener = new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                saveSettings();
            }
        };

        java.awt.event.ActionListener actionListener = new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (evt.getSource() == jComboBox3) {
                    updateCurrencyLabel(); 
                }
                
                saveSettings(); 
                
                if (evt.getSource() instanceof javax.swing.JTextField) {
                   ((javax.swing.JTextField)evt.getSource()).transferFocus(); 
                }
            }
        };

        jTextFieldSalary.addFocusListener(focusLostListener);
        jTextFieldSalary.addActionListener(actionListener);

        jTextFieldHours.addFocusListener(focusLostListener);
        jTextFieldHours.addActionListener(actionListener);

        jTextFieldDays.addFocusListener(focusLostListener);
        jTextFieldDays.addActionListener(actionListener);

        jTextFieldInvest.addFocusListener(focusLostListener);
        jTextFieldInvest.addActionListener(actionListener);

        jTextField2.addFocusListener(focusLostListener);
        jTextField2.addActionListener(actionListener);

        jComboBox3.addActionListener(actionListener); 
    }
    private void updateCurrencyLabel() {
        String selected = (String) jComboBox3.getSelectedItem();
        if (selected != null && selected.contains("(") && selected.contains(")")) {
            String symbol = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
            currentcylabel.setText(symbol); 
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

        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        roundedPanel1 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel9 = new javax.swing.JLabel();
        roundedPanel3 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jTextFieldSalary = new javax.swing.JTextField();
        currentcylabel = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        roundedPanel2 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        roundedPanel7 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jTextFieldHours = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        roundedPanel8 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jTextFieldDays = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        roundedPanel9 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jTextFieldInvest = new javax.swing.JTextField();
        roundedPanel5 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox<>();
        roundedPanel6 = new com.mycompany.cs318_finalproject_buynevercry.RoundedPanel();
        jTextField2 = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();

        jLabel2.setFont(new java.awt.Font("Inter 18pt", 0, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(82, 82, 82));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Convert yearly salary to hourly rate with customizable work assumptions");

        jLabel3.setFont(new java.awt.Font("Inter 18pt", 0, 30)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Setting Salary & Hourly Rate");

        roundedPanel1.setPreferredSize(new java.awt.Dimension(672, 162));

        jLabel9.setFont(new java.awt.Font("Inter 18pt", 0, 14)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(64, 64, 64));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("I know my yearly salary");

        roundedPanel3.setCornerRadius(8);
        roundedPanel3.setPreferredSize(new java.awt.Dimension(319, 54));

        jTextFieldSalary.setFont(new java.awt.Font("Inter 18pt", 0, 18)); // NOI18N
        jTextFieldSalary.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldSalary.setBorder(null);
        jTextFieldSalary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSalaryActionPerformed(evt);
            }
        });

        currentcylabel.setFont(new java.awt.Font("Inter 18pt", 0, 14)); // NOI18N
        currentcylabel.setText("$");

        javax.swing.GroupLayout roundedPanel3Layout = new javax.swing.GroupLayout(roundedPanel3);
        roundedPanel3.setLayout(roundedPanel3Layout);
        roundedPanel3Layout.setHorizontalGroup(
            roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel3Layout.createSequentialGroup()
                .addContainerGap(23, Short.MAX_VALUE)
                .addComponent(currentcylabel)
                .addGap(18, 18, 18)
                .addComponent(jTextFieldSalary, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(45, 45, 45))
        );
        roundedPanel3Layout.setVerticalGroup(
            roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldSalary, javax.swing.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
                    .addComponent(currentcylabel))
                .addContainerGap())
        );

        jLabel10.setFont(new java.awt.Font("Inter 18pt", 0, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(64, 64, 64));
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("Enter Your Yearly Salary");

        javax.swing.GroupLayout roundedPanel1Layout = new javax.swing.GroupLayout(roundedPanel1);
        roundedPanel1.setLayout(roundedPanel1Layout);
        roundedPanel1Layout.setHorizontalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(roundedPanel1Layout.createSequentialGroup()
                .addGap(169, 169, 169)
                .addComponent(roundedPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(184, Short.MAX_VALUE))
            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        roundedPanel1Layout.setVerticalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel9)
                .addGap(10, 10, 10)
                .addComponent(jLabel10)
                .addGap(12, 12, 12)
                .addComponent(roundedPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        roundedPanel2.setBorderThickness(0.0F);
        roundedPanel2.setPanelColor(new java.awt.Color(242, 242, 242));
        roundedPanel2.setPreferredSize(new java.awt.Dimension(672, 162));

        jLabel4.setFont(new java.awt.Font("Inter 18pt", 0, 18)); // NOI18N
        jLabel4.setText("Work Assumptions ");

        jLabel5.setFont(new java.awt.Font("Inter 18pt", 0, 12)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(64, 64, 64));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Hours per day (Default: 8)");

        roundedPanel7.setCornerRadius(4);
        roundedPanel7.setPreferredSize(new java.awt.Dimension(304, 42));

        jTextFieldHours.setFont(new java.awt.Font("Inter 18pt", 0, 18)); // NOI18N
        jTextFieldHours.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldHours.setBorder(null);
        jTextFieldHours.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldHoursActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel7Layout = new javax.swing.GroupLayout(roundedPanel7);
        roundedPanel7.setLayout(roundedPanel7Layout);
        roundedPanel7Layout.setHorizontalGroup(
            roundedPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldHours)
                .addContainerGap())
        );
        roundedPanel7Layout.setVerticalGroup(
            roundedPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldHours, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel6.setFont(new java.awt.Font("Inter 18pt", 0, 12)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(64, 64, 64));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Days per week (Default: 8)");

        roundedPanel8.setCornerRadius(4);
        roundedPanel8.setPreferredSize(new java.awt.Dimension(304, 42));

        jTextFieldDays.setFont(new java.awt.Font("Inter 18pt", 0, 18)); // NOI18N
        jTextFieldDays.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldDays.setBorder(null);
        jTextFieldDays.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldDaysActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel8Layout = new javax.swing.GroupLayout(roundedPanel8);
        roundedPanel8.setLayout(roundedPanel8Layout);
        roundedPanel8Layout.setHorizontalGroup(
            roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldDays, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                .addContainerGap())
        );
        roundedPanel8Layout.setVerticalGroup(
            roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldDays, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel14.setFont(new java.awt.Font("Inter 18pt", 0, 12)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(64, 64, 64));
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("Investment Return % (Default: 3)");

        roundedPanel9.setCornerRadius(4);
        roundedPanel9.setPreferredSize(new java.awt.Dimension(304, 42));

        jTextFieldInvest.setFont(new java.awt.Font("Inter 18pt", 0, 18)); // NOI18N
        jTextFieldInvest.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldInvest.setBorder(null);
        jTextFieldInvest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldInvestActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel9Layout = new javax.swing.GroupLayout(roundedPanel9);
        roundedPanel9.setLayout(roundedPanel9Layout);
        roundedPanel9Layout.setHorizontalGroup(
            roundedPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldInvest)
                .addContainerGap())
        );
        roundedPanel9Layout.setVerticalGroup(
            roundedPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldInvest, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout roundedPanel2Layout = new javax.swing.GroupLayout(roundedPanel2);
        roundedPanel2.setLayout(roundedPanel2Layout);
        roundedPanel2Layout.setHorizontalGroup(
            roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel2Layout.createSequentialGroup()
                .addContainerGap(24, Short.MAX_VALUE)
                .addGroup(roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addGroup(roundedPanel2Layout.createSequentialGroup()
                        .addGroup(roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                            .addComponent(roundedPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE))
                        .addGap(36, 36, 36)
                        .addGroup(roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(roundedPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(36, 36, 36)
                        .addGroup(roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                            .addComponent(roundedPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE))))
                .addContainerGap(53, Short.MAX_VALUE))
        );
        roundedPanel2Layout.setVerticalGroup(
            roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel2Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addGroup(roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(roundedPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(roundedPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(roundedPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(16, 16, 16)
                        .addGroup(roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(roundedPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(roundedPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(roundedPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(roundedPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        roundedPanel5.setBorderThickness(0.0F);
        roundedPanel5.setPanelColor(new java.awt.Color(242, 242, 242));
        roundedPanel5.setPreferredSize(new java.awt.Dimension(672, 162));

        jLabel11.setFont(new java.awt.Font("Inter 18pt", 0, 12)); // NOI18N
        jLabel11.setText("Currency Settings");

        jLabel12.setFont(new java.awt.Font("Inter 18pt", 0, 12)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(64, 64, 64));
        jLabel12.setText("Currency");

        jComboBox3.setFont(new java.awt.Font("Inter 18pt", 0, 14)); // NOI18N
        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "THB (฿)", "USD ($)", "EUR (€)", "JPY (¥)", "GBP (£)", "CNY (¥)" }));
        jComboBox3.setBorder(null);
        jComboBox3.setPreferredSize(new java.awt.Dimension(304, 41));

        roundedPanel6.setCornerRadius(4);
        roundedPanel6.setPreferredSize(new java.awt.Dimension(304, 42));

        jTextField2.setFont(new java.awt.Font("Inter 18pt", 0, 18)); // NOI18N
        jTextField2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField2.setBorder(null);
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel6Layout = new javax.swing.GroupLayout(roundedPanel6);
        roundedPanel6.setLayout(roundedPanel6Layout);
        roundedPanel6Layout.setHorizontalGroup(
            roundedPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                .addContainerGap())
        );
        roundedPanel6Layout.setVerticalGroup(
            roundedPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel13.setFont(new java.awt.Font("Inter 18pt", 0, 12)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(64, 64, 64));
        jLabel13.setText("Custom Label (Optional)");

        javax.swing.GroupLayout roundedPanel5Layout = new javax.swing.GroupLayout(roundedPanel5);
        roundedPanel5.setLayout(roundedPanel5Layout);
        roundedPanel5Layout.setHorizontalGroup(
            roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel5Layout.createSequentialGroup()
                .addContainerGap(24, Short.MAX_VALUE)
                .addGroup(roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addGroup(roundedPanel5Layout.createSequentialGroup()
                        .addGroup(roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel12))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addComponent(roundedPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(29, 29, 29))
        );
        roundedPanel5Layout.setVerticalGroup(
            roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel5Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jLabel11)
                .addGap(16, 16, 16)
                .addGroup(roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jLabel13))
                .addGap(8, 8, 8)
                .addGroup(roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(roundedPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jComboBox3, javax.swing.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE))
                .addContainerGap(31, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(57, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 678, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 672, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(roundedPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 667, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(roundedPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(roundedPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(57, 57, 57))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(57, 57, 57)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)
                .addComponent(roundedPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35)
                .addComponent(roundedPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(57, 57, 57))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextFieldSalaryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSalaryActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldSalaryActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jTextFieldHoursActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldHoursActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldHoursActionPerformed

    private void jTextFieldDaysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldDaysActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldDaysActionPerformed

    private void jTextFieldInvestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldInvestActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldInvestActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel currentcylabel;
    private javax.swing.JComboBox<String> jComboBox3;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextFieldDays;
    private javax.swing.JTextField jTextFieldHours;
    private javax.swing.JTextField jTextFieldInvest;
    private javax.swing.JTextField jTextFieldSalary;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel1;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel2;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel3;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel5;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel6;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel7;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel8;
    private com.mycompany.cs318_finalproject_buynevercry.RoundedPanel roundedPanel9;
    // End of variables declaration//GEN-END:variables
}
