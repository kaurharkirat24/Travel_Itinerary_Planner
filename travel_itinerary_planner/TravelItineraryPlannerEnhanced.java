import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TravelItineraryPlannerEnhanced extends JFrame {
    // Constants
    private static final int ARC_RADIUS = 20;
    private static final Font LABEL_FONT = new Font("Roboto", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Roboto", Font.BOLD, 14);
    private static final Font AREA_FONT = new Font("Roboto", Font.PLAIN, 16);

    private JTextField sourceField, destinationField, budgetTransport, budgetFood, budgetTickets;
    private JTextArea itineraryArea, tipsArea, remindersArea;
    private JButton generateBtn, saveBtn, loadBtn, remindBtn, estimateBtn, gptBtn, mapBtn;
    private JComboBox<String> daysCombo;
    private JFileChooser fileChooser;
    private JCheckBox[] attractionCBs, interestCBs;
    private String transportMode;
    private File remindersFile = new File("reminders.txt");

    // Store custom days if selected
    private int customDays = -1;

    public TravelItineraryPlannerEnhanced() {
        setLookAndFeel();
        setTitle("AI-Powered Travel Itinerary Planner");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Color bg = new Color(245, 245, 250);
        Color accent = new Color(30, 144, 255);
        Color secondary = new Color(255, 165, 0);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, bg, 0, getHeight(), new Color(200, 220, 255));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top panel
        JPanel topPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        topPanel.setOpaque(false);

        // Row 1: Inputs
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row1.setOpaque(false);
        sourceField = new JTextField(15);
        destinationField = new JTextField(15);
        String[] days = {"1 Day", "2 Days", "3 Days", "4 Days", "5 Days", "6 Days", "7 Days", "Custom"};
        daysCombo = new JComboBox<>(days);

        // Custom days logic
        daysCombo.addActionListener(e -> {
            int idx = daysCombo.getSelectedIndex();
            if (idx == daysCombo.getItemCount() - 1) { // "Custom" selected
                String input = JOptionPane.showInputDialog(this, "Enter number of days:", "Custom Days", JOptionPane.PLAIN_MESSAGE);
                try {
                    int enteredDays = Integer.parseInt(input);
                    if (enteredDays > 0) {
                        customDays = enteredDays;
                        String customLabel = enteredDays + (enteredDays == 1 ? " Day" : " Days");
                        // Remove previous custom if exists (before "Custom")
                        int customIdx = daysCombo.getItemCount() - 2;
                        if (customIdx >= 0 && !daysCombo.getItemAt(customIdx).equals("7 Days")) {
                            daysCombo.removeItemAt(customIdx);
                        }
                        daysCombo.insertItemAt(customLabel, daysCombo.getItemCount() - 1);
                        daysCombo.setSelectedIndex(daysCombo.getItemCount() - 2);
                    } else {
                        customDays = -1;
                        daysCombo.setSelectedIndex(0);
                    }
                } catch (Exception ex) {
                    customDays = -1;
                    daysCombo.setSelectedIndex(0);
                }
            } else {
                customDays = -1;
            }
        });

        generateBtn = createStyledButton("Generate Itinerary", accent);
        row1.add(createStyledLabel("Source: "));
        row1.add(sourceField);
        row1.add(createStyledLabel("Destination: "));
        row1.add(destinationField);
        row1.add(createStyledLabel("Duration: "));
        row1.add(daysCombo);
        row1.add(generateBtn);

        // Row 2: Attractions
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2.setOpaque(false);
        String[] attractionLabels = {"Museums", "Parks", "Cafes", "Street Food", "Fine Dining"};
        attractionCBs = createCheckBoxes(attractionLabels, row2);

        // Row 3: Interests
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row3.setOpaque(false);
        String[] interestLabels = {"Food", "Adventure", "Relaxation", "Cultural Sites", "Shopping"};
        interestCBs = createCheckBoxes(interestLabels, row3);
        gptBtn = createStyledButton("Get GPT Suggestions", accent);
        row3.add(gptBtn);

        topPanel.add(row1);
        topPanel.add(row2);
        topPanel.add(row3);

        // Itinerary Area
        itineraryArea = new JTextArea();
        itineraryArea.setFont(AREA_FONT);
        itineraryArea.setLineWrap(true);
        itineraryArea.setWrapStyleWord(true);
        itineraryArea.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accent), "Itinerary",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            BUTTON_FONT, accent
        ));
        JScrollPane scrollPane = new JScrollPane(itineraryArea);
        scrollPane.setPreferredSize(new Dimension(1150, 300));

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setOpaque(false);
        saveBtn = createStyledButton("Save Itinerary", secondary);
        loadBtn = createStyledButton("Load Itinerary", secondary);
        remindBtn = createStyledButton("Add Reminder", secondary);
        estimateBtn = createStyledButton("Estimate Budget", secondary);
        mapBtn = createStyledButton("View Map", secondary);
        for (JButton btn : new JButton[]{saveBtn, loadBtn, remindBtn, estimateBtn, mapBtn}) {
            buttonPanel.add(btn);
        }

        // Budget Panel
        JPanel budgetPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        budgetPanel.setOpaque(false);
        budgetPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accent), "Daily Budget (INR)",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            BUTTON_FONT, accent
        ));
        budgetTransport = new JTextField("0");
        budgetFood = new JTextField("0");
        budgetTickets = new JTextField("0");
        budgetPanel.add(createStyledLabel("Transport:"));
        budgetPanel.add(budgetTransport);
        budgetPanel.add(createStyledLabel("Food:"));
        budgetPanel.add(budgetFood);
        budgetPanel.add(createStyledLabel("Tickets:"));
        budgetPanel.add(budgetTickets);

        // Tips Area
        tipsArea = new JTextArea(5, 30);
        tipsArea.setFont(new Font("Roboto", Font.PLAIN, 14));
        tipsArea.setLineWrap(true);
        tipsArea.setWrapStyleWord(true);
        tipsArea.setEditable(false);
        tipsArea.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accent), "Travel Tips",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            BUTTON_FONT, accent
        ));

        // Reminders Area
        remindersArea = new JTextArea(5, 30);
        remindersArea.setFont(new Font("Roboto", Font.PLAIN, 14));
        remindersArea.setLineWrap(true);
        remindersArea.setWrapStyleWord(true);
        remindersArea.setEditable(false);
        remindersArea.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accent), "Reminders",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            BUTTON_FONT, accent
        ));
        JScrollPane remindersScrollPane = new JScrollPane(remindersArea);

        // West Panel
        JPanel westPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        westPanel.setOpaque(false);
        westPanel.add(tipsArea);
        westPanel.add(remindersScrollPane);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(budgetPanel, BorderLayout.EAST);
        mainPanel.add(westPanel, BorderLayout.WEST);

        fileChooser = new JFileChooser();

        // Action listeners
        generateBtn.addActionListener(e -> generateItinerary());
        saveBtn.addActionListener(e -> saveToFile());
        loadBtn.addActionListener(e -> loadFromFile());
        gptBtn.addActionListener(e -> showGPTSuggestions());
        remindBtn.addActionListener(e -> showReminderDialog());
        estimateBtn.addActionListener(e -> estimateBudget());
        mapBtn.addActionListener(e -> showMap());

        loadReminders();
        setContentPane(mainPanel);
    }

    private void setLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private JCheckBox[] createCheckBoxes(String[] labels, JPanel panel) {
        JCheckBox[] boxes = new JCheckBox[labels.length];
        panel.add(createStyledLabel(panel.getComponentCount() == 0 ? "Attractions: " : "Interests: "));
        for (int i = 0; i < labels.length; i++) {
            boxes[i] = new JCheckBox(labels[i]);
            panel.add(boxes[i]);
        }
        return boxes;
    }

    private int getSelectedDays() {
        if (customDays > 0 && daysCombo.getSelectedIndex() == daysCombo.getItemCount() - 2) {
            return customDays;
        } else {
            // Parse the number from the selected item (e.g., "3 Days")
            String selected = (String) daysCombo.getSelectedItem();
            try {
                return Integer.parseInt(selected.split(" ")[0]);
            } catch (Exception e) {
                return 1;
            }
        }
    }

    private void generateItinerary() {
        String destination = destinationField.getText().trim();
        int days = getSelectedDays();

        if (destination.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a destination.");
            return;
        }

        String[] transportOptions = {"Car", "Public Transport", "Flight", "Walking", "Bicycle"};
        transportMode = (String) JOptionPane.showInputDialog(
            this, "Select mode of transportation:", "Transportation",
            JOptionPane.QUESTION_MESSAGE, null, transportOptions, transportOptions[0]
        );
        if (transportMode == null) {
            JOptionPane.showMessageDialog(this, "Transportation mode not selected.");
            return;
        }

        String[][] activities = {
            {"Visit Historical Museum", "Explore Art Gallery", "Tour Science Museum"},
            {"Morning Walk in City Park", "Picnic at Botanical Garden", "Hike in Nature Reserve"},
            {"Breakfast at Local Cafe", "Coffee Tasting at Specialty Cafe", "Dessert at Patisserie"},
            {"Street Food Tour", "Fine Dining Experience", "Local Cooking Class", "Food Market Exploration"},
            {"Kayaking Adventure", "Rock Climbing", "Ziplining Tour", "Paragliding Experience"},
            {"Spa Day", "Beach Relaxation", "Yoga Retreat", "Meditation Session"},
            {"Street Food Stall Crawl", "Night Market Food Tour", "Local Snack Tasting"},
            {"Dinner at Michelin-Star Restaurant", "Wine Tasting Dinner", "Gourmet Tasting Menu"},
            {"Visit Ancient Ruins", "Tour Historical Landmarks", "Attend Cultural Festival"},
            {"Shop at Local Markets", "Visit Luxury Malls", "Explore Artisan Boutiques"},
            {"Live Music Event", "Rooftop Bar Visit", "Cultural Dance Show", "Night Market Stroll"}
        };

        StringBuilder plan = new StringBuilder("\nItinerary for " + destination + " (" + days + " day(s))\n");
        plan.append("Mode of Transportation: ").append(transportMode).append("\n------------------------------\n");
        for (int i = 1; i <= days; i++) {
            plan.append("Day ").append(i).append(":\n");
            if (attractionCBs[0].isSelected()) plan.append("  - ").append(activities[0][(i - 1) % activities[0].length]).append("\n");
            if (attractionCBs[1].isSelected()) plan.append("  - ").append(activities[1][(i - 1) % activities[1].length]).append("\n");
            if (attractionCBs[2].isSelected()) plan.append("  - ").append(activities[2][(i - 1) % activities[2].length]).append("\n");
            if (interestCBs[0].isSelected()) plan.append("  - ").append(activities[3][(i - 1) % activities[3].length]).append("\n");
            if (interestCBs[1].isSelected()) plan.append("  - ").append(activities[4][(i - 1) % activities[4].length]).append("\n");
            if (interestCBs[2].isSelected()) plan.append("  - ").append(activities[5][(i - 1) % activities[5].length]).append("\n");
            if (attractionCBs[3].isSelected()) plan.append("  - ").append(activities[6][(i - 1) % activities[6].length]).append("\n");
            if (attractionCBs[4].isSelected()) plan.append("  - ").append(activities[7][(i - 1) % activities[7].length]).append("\n");
            if (interestCBs[3].isSelected()) plan.append("  - ").append(activities[8][(i - 1) % activities[8].length]).append("\n");
            if (interestCBs[4].isSelected()) plan.append("  - ").append(activities[9][(i - 1) % activities[9].length]).append("\n");
            plan.append("  - Evening: ").append(activities[10][(i - 1) % activities[10].length]).append("\n\n");
        }
        itineraryArea.setText(plan.toString());
    }

    private void saveToFile() {
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
                writer.write(itineraryArea.getText());
                JOptionPane.showMessageDialog(this, "Itinerary saved.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage());
            }
        }
    }

    private void loadFromFile() {
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileChooser.getSelectedFile()))) {
                itineraryArea.setText("");
                String line;
                while ((line = reader.readLine()) != null) itineraryArea.append(line + "\n");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading: " + e.getMessage());
            }
        }
    }

    private double estimateDistance(String source, String destination) {
        String key = source.toLowerCase() + "-" + destination.toLowerCase();
        return switch (key) {
            case "delhi-mumbai", "mumbai-delhi" -> 1400;
            case "delhi-goa", "goa-delhi" -> 1800;
            case "mumbai-goa", "goa-mumbai" -> 600;
            default -> new Random().nextInt(1000) + 100;
        };
    }

    private void estimateBudget() {
        try {
            String source = sourceField.getText().trim();
            String destination = destinationField.getText().trim();
            int days = getSelectedDays();

            if (source.isEmpty() || destination.isEmpty() || transportMode == null) {
                JOptionPane.showMessageDialog(this, "Please enter source, destination, and generate an itinerary.");
                return;
            }

            double distance = estimateDistance(source, destination);

            double transportCostPerTrip = switch (transportMode) {
                case "Flight" -> distance * 5;
                case "Car" -> distance * 2;
                case "Public Transport" -> distance * 0.5;
                case "Walking" -> 0;
                case "Bicycle" -> distance * 0.1;
                default -> distance * 1;
            };

            double totalTransportCost = transportCostPerTrip * 2;
            double foodCostPerDay = (interestCBs[0].isSelected() || attractionCBs[3].isSelected() || attractionCBs[4].isSelected()) ? 1500 : 800;
            double ticketCostPerDay = (attractionCBs[0].isSelected() || interestCBs[1].isSelected() || interestCBs[3].isSelected()) ? 1000 : 500;

            double totalFoodCost = foodCostPerDay * days;
            double totalTicketCost = ticketCostPerDay * days;
            double total = totalTransportCost + totalFoodCost + totalTicketCost;

            budgetTransport.setText(String.format("%.2f", totalTransportCost));
            budgetFood.setText(String.format("%.2f", totalFoodCost));
            budgetTickets.setText(String.format("%.2f", totalTicketCost));

            String message = String.format(
                "Estimated Budget for %d days from %s to %s (Distance: %.0f km):\n" +
                "Transport (%s, round trip): INR %.2f\n" +
                "Food: INR %.2f\n" +
                "Tickets: INR %.2f\n" +
                "Total: INR %.2f",
                days, source, destination, distance, transportMode, totalTransportCost,
                totalFoodCost, totalTicketCost, total
            );
            JOptionPane.showMessageDialog(this, message);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error estimating budget: " + e.getMessage());
        }
    }

    private void showMap() {
        String destination = destinationField.getText().trim();
        if (destination.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a destination.");
            return;
        }
        try {
            String mapUrl = "https://www.google.com/maps/search/" + destination.replace(" ", "+");
            Desktop.getDesktop().browse(new URI(mapUrl));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error opening map: " + e.getMessage());
        }
    }

    private void showGPTSuggestions() {
        String[] tips = {
            "Carry a reusable water bottle to stay hydrated and save money.",
            "Download offline maps for your destination to navigate without internet.",
            "Check local customs and dress codes to respect cultural norms.",
            "Always have a power bank for your devices during long days out.",
            "Book popular attractions in advance to avoid long queues.",
            "Try local street food, but ensure it's from reputable vendors.",
            "Keep digital and physical copies of important travel documents."
        };
        Random rand = new Random();
        StringBuilder selectedTips = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            selectedTips.append("- ").append(tips[rand.nextInt(tips.length)]).append("\n");
        }
        tipsArea.setText(selectedTips.toString());
    }

    private void showReminderDialog() {
        String reminder = JOptionPane.showInputDialog(this, "Enter a reminder:");
        if (reminder != null && !reminder.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = now.format(formatter);
            String formattedReminder = "[" + timestamp + "] " + reminder;

            if (remindersArea.getText().isEmpty()) {
                remindersArea.setText(formattedReminder);
            } else {
                remindersArea.append("\n" + formattedReminder);
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(remindersFile, true))) {
                writer.println(formattedReminder);
                JOptionPane.showMessageDialog(this, "Reminder saved: " + reminder);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving reminder: " + e.getMessage());
            }
        }
    }

    private void loadReminders() {
        if (remindersFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(remindersFile))) {
                StringBuilder remindersText = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (remindersText.length() > 0) remindersText.append("\n");
                    remindersText.append(line);
                }
                remindersArea.setText(remindersText.toString());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading reminders: " + e.getMessage());
            }
        }
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC_RADIUS, ARC_RADIUS);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        button.setFont(BUTTON_FONT);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setContentAreaFilled(false);
        return button;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(new Color(30, 144, 255));
        return label;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TravelItineraryPlannerEnhanced().setVisible(true));
    }
}