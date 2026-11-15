package hospital;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ReportsPage extends JPanel {
    private final Ui.TextArea area = new Ui.TextArea();

    public ReportsPage(HospitalController ctl) {
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        Ui.Button snap = new Ui.Button("Generate snapshot", Icons.REPORT, Ui.Kind.PRIMARY);
        Ui.Button export = new Ui.Button("Export...", Icons.DOWNLOAD, Ui.Kind.GHOST);
        bar.add(snap); bar.add(export);
        add(bar, BorderLayout.NORTH);

        Ui.Card card = new Ui.Card();
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane sp = Ui.scroll(area, () -> Theme.surface);
        card.add(sp, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        area.setText("No report yet.\n\nClick 'Generate snapshot' for a live summary, or stop monitoring\nto create the final report automatically (saved as final_report.txt).");

        snap.addActionListener(e -> { area.setText(ctl.buildReport(false)); area.setCaretPosition(0); });
        export.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("vitals_report.txt"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    ctl.exportReport(fc.getSelectedFile(), area.getText());
                    Ui.confirm(this, "Report exported", "Saved to\n" + fc.getSelectedFile().getAbsolutePath(), "OK", false, false);
                } catch (IOException ex) {
                    Ui.confirm(this, "Export failed", ex.getMessage(), "OK", true, false);
                }
            }
        });
    }

    public void setReport(String text) { area.setText(text); area.setCaretPosition(0); }
}
