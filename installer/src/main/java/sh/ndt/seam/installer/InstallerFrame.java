package sh.ndt.seam.installer;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class InstallerFrame extends JFrame {

    private final JComboBox<String> mcVersionBox;
    private final JComboBox<String> seamVersionBox;
    private final JTextField dirField;
    private final JTextArea logArea;
    private final JButton installBtn;

    private final JPanel installationsListPanel;
    private final JLabel installationsStatus;
    private final JTabbedPane tabs;

    private VersionManifest manifest;
    private List<VersionManifest.VersionEntry> currentSeamVersions = List.of();

    public InstallerFrame() {
        super("Seam Installer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // ── header ────────────────────────────────────────────────────────────
        JLabel title = new JLabel("Seam Mod Loader");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setBorder(new EmptyBorder(14, 16, 10, 16));

        // ── install tab ───────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 1, 0, Color.LIGHT_GRAY),
            new EmptyBorder(10, 16, 10, 16)));

        form.add(new JLabel("Minecraft version:"), lc(0));
        mcVersionBox = new JComboBox<>(new String[]{"Loading…"});
        mcVersionBox.setEnabled(false);
        form.add(mcVersionBox, fc(0));

        form.add(new JLabel("Seam version:"), lc(1));
        seamVersionBox = new JComboBox<>(new String[]{"Loading…"});
        seamVersionBox.setEnabled(false);
        form.add(seamVersionBox, fc(1));

        form.add(new JLabel("Directory:"), lc(2));
        dirField = new JTextField(MinecraftFinder.find().toString());
        JButton browseBtn = new JButton("...");
        browseBtn.setMargin(new Insets(1, 6, 1, 6));
        browseBtn.addActionListener(e -> browse());
        JPanel dirRow = new JPanel(new BorderLayout(4, 0));
        dirRow.add(dirField, BorderLayout.CENTER);
        dirRow.add(browseBtn, BorderLayout.EAST);
        form.add(dirRow, fc(2));

        mcVersionBox.addActionListener(e -> {
            if (manifest != null) populateSeamVersions((String) mcVersionBox.getSelectedItem());
        });

        logArea = new JTextArea(7, 52);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setBackground(new Color(245, 245, 245));
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new EmptyBorder(8, 16, 8, 16));
        logPanel.add(new JScrollPane(logArea));

        JPanel installButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        installButtons.setBorder(new MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> System.exit(0));
        installBtn = new JButton("Install");
        installBtn.setPreferredSize(new Dimension(90, installBtn.getPreferredSize().height));
        installBtn.addActionListener(e -> install());
        installButtons.add(cancelBtn);
        installButtons.add(installBtn);

        JPanel installTab = new JPanel(new BorderLayout());
        installTab.add(form, BorderLayout.NORTH);
        installTab.add(logPanel, BorderLayout.CENTER);
        installTab.add(installButtons, BorderLayout.SOUTH);

        // ── installations tab ─────────────────────────────────────────────────
        installationsListPanel = new JPanel();
        installationsListPanel.setLayout(new BoxLayout(installationsListPanel, BoxLayout.Y_AXIS));

        JScrollPane listScroll = new JScrollPane(installationsListPanel);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.getVerticalScrollBar().setUnitIncrement(16);

        installationsStatus = new JLabel(" ");
        installationsStatus.setFont(installationsStatus.getFont().deriveFont(Font.ITALIC, 11f));
        installationsStatus.setBorder(new EmptyBorder(4, 16, 4, 16));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshInstallations());
        JPanel manageHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        manageHeader.setBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        manageHeader.add(installationsStatus);
        manageHeader.add(refreshBtn);

        JPanel manageTab = new JPanel(new BorderLayout());
        manageTab.add(manageHeader, BorderLayout.NORTH);
        manageTab.add(listScroll, BorderLayout.CENTER);

        // ── tabs ──────────────────────────────────────────────────────────────
        tabs = new JTabbedPane();
        tabs.addTab("Install", installTab);
        tabs.addTab("Installations", manageTab);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) refreshInstallations();
        });

        // ── assemble ──────────────────────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout());
        content.add(title, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);

        setContentPane(content);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        initLog();
        fetchManifestAsync();
    }

    // ── version manifest ──────────────────────────────────────────────────────

    private void fetchManifestAsync() {
        installBtn.setEnabled(false);
        new Thread(() -> {
            String url = VersionManifest.MANIFEST_URL;
            log(url.isEmpty() ? "Loading bundled version list…"
                              : "Fetching version list from " + url + " …");
            VersionManifest m = VersionManifest.load();
            SwingUtilities.invokeLater(() -> applyManifest(m));
        }, "manifest-fetch").start();
    }

    private void applyManifest(VersionManifest m) {
        manifest = m;
        mcVersionBox.removeAllItems();
        m.mcVersions().forEach(mcVersionBox::addItem);
        mcVersionBox.setEnabled(true);
        seamVersionBox.setEnabled(true);
        populateSeamVersions((String) mcVersionBox.getSelectedItem());
        installBtn.setEnabled(true);
        log("Version list loaded.");
    }

    private void populateSeamVersions(String mcVersion) {
        currentSeamVersions = manifest.seamVersions(mcVersion);
        seamVersionBox.removeAllItems();
        for (int i = 0; i < currentSeamVersions.size(); i++) {
            seamVersionBox.addItem(currentSeamVersions.get(i).displayLabel(i == 0));
        }
    }

    private void initLog() {
        log("Seam Installer ready.");
        Path mc = MinecraftFinder.find();
        if (Files.isDirectory(mc)) {
            log("Detected Minecraft directory: " + mc);
        } else {
            log("Minecraft directory not found — please select manually.");
        }
        log("Java " + System.getProperty("java.version") +
            " (" + System.getProperty("os.name") + ")");
    }

    // ── install ───────────────────────────────────────────────────────────────

    private void browse() {
        JFileChooser fc = new JFileChooser(dirField.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select Minecraft directory");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            dirField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void install() {
        installBtn.setEnabled(false);
        logArea.setText("");
        var mcDir = Paths.get(dirField.getText().trim());
        int idx = seamVersionBox.getSelectedIndex();
        String selectedMc = (String) mcVersionBox.getSelectedItem();
        if (idx < 0 || idx >= currentSeamVersions.size()) {
            log("ERROR: no version selected.");
            installBtn.setEnabled(true);
            return;
        }
        var entry = currentSeamVersions.get(idx);

        new Thread(() -> {
            try {
                new Installer(mcDir, this::log).install((String) mcVersionBox.getSelectedItem(), entry);
                SwingUtilities.invokeLater(() ->
                    logBanner("INSTALLATION COMPLETE",
                        "Open the Minecraft Launcher and select the \"Seam b1.8.1\" profile."));
            } catch (Exception ex) {
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                log("ERROR: " + ex.getMessage());
                log(sw.toString());
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Installation failed:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> installBtn.setEnabled(true));
            }
        }, "installer").start();
    }

    // ── installations tab ─────────────────────────────────────────────────────

    private void refreshInstallations() {
        Path mcDir = Paths.get(dirField.getText().trim());
        List<InstallationRecord> records = Installer.scan(mcDir);

        SwingUtilities.invokeLater(() -> {
            installationsListPanel.removeAll();

            if (records.isEmpty()) {
                JLabel none = new JLabel("No Seam installations found in " + mcDir);
                none.setForeground(Color.DARK_GRAY);
                none.setBorder(new EmptyBorder(20, 20, 20, 20));
                installationsListPanel.add(none);
                installationsStatus.setText("No installations found");
            } else {
                installationsStatus.setText(records.size() + " installation(s) found");
                for (InstallationRecord rec : records) {
                    installationsListPanel.add(buildRow(rec));
                }
            }

            installationsListPanel.revalidate();
            installationsListPanel.repaint();
        });
    }

    private JPanel buildRow(InstallationRecord rec) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
            new EmptyBorder(10, 16, 10, 16)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 24));

        // left: profile name + details
        JPanel info = new JPanel(new GridLayout(2, 1, 2, 2));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(rec.profileName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        info.add(nameLabel);

        String details = "Version: " + rec.versionId()
            + "  •  MC: " + rec.inheritsFrom()
            + "  •  Agent: " + (rec.agentExists() ? "present" : "missing");
        JLabel detailLabel = new JLabel(details);
        detailLabel.setFont(detailLabel.getFont().deriveFont(11f));
        detailLabel.setForeground(rec.agentExists() ? Color.DARK_GRAY : new Color(180, 40, 40));
        info.add(detailLabel);

        row.add(info, BorderLayout.CENTER);

        // right: edit + remove buttons
        JButton editBtn = new JButton("Rename");
        editBtn.addActionListener(e -> promptRename(rec));
        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> confirmRemove(rec));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(editBtn);
        btnPanel.add(removeBtn);
        row.add(btnPanel, BorderLayout.EAST);

        return row;
    }

    private void promptRename(InstallationRecord rec) {
        String newName = (String) JOptionPane.showInputDialog(this,
            "New name for this installation:",
            "Rename", JOptionPane.PLAIN_MESSAGE, null, null, rec.profileName());
        if (newName == null || newName.isBlank() || newName.equals(rec.profileName())) return;

        Path mcDir = Paths.get(dirField.getText().trim());
        new Thread(() -> {
            try {
                new Installer(mcDir, msg -> {}).rename(rec, newName.trim());
                SwingUtilities.invokeLater(this::refreshInstallations);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Rename failed:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "renamer").start();
    }

    private void confirmRemove(InstallationRecord rec) {
        int choice = JOptionPane.showConfirmDialog(this,
            "Remove \"" + rec.profileName() + "\" (" + rec.versionId() + ")?\n\n"
                + "This deletes the launcher profile and versions/" + rec.versionId() + "/.\n"
                + "Mods in seam/mods/ are not touched.",
            "Confirm removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) return;

        Path mcDir = Paths.get(dirField.getText().trim());
        new Thread(() -> {
            try {
                new Installer(mcDir, this::log).remove(rec);
                SwingUtilities.invokeLater(() -> {
                    logBanner("DELETED", "\"" + rec.profileName() + "\" has been removed.");
                    tabs.setSelectedIndex(0);
                    refreshInstallations();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Removal failed:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "remover").start();
    }

    private void logBanner(String title, String body) {
        String inner = "   " + title + "   ";
        String border = "*".repeat(inner.length() + 2);
        log("");
        log(border);
        log("*" + inner + "*");
        log(border);
        log(body);
        log("");
    }

    private void log(String msg) {
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> log(msg));
        }
    }

    // ── GridBagConstraints helpers ────────────────────────────────────────────

    private static GridBagConstraints lc(int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 4, 10);
        return c;
    }

    private static GridBagConstraints fc(int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1; c.gridy = row;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(4, 0, 4, 0);
        return c;
    }
}
