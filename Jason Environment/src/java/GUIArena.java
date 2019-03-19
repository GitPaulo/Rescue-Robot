import javax.swing.JFrame;
import java.awt.GridLayout;
import javax.swing.JPanel;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;
import javax.swing.JTextArea;

/**
 * Main GUI Class for the arena
 * 
 * @author group 16
 */
public class GUIArena {
    private Arena.Cell currentPosition;
    private float currentHeading;
    private final Arena arena;
    private String status;
    private String vr;
    private String vrm;

    private GUIArenaCell[][] gridButtons;
    private Arena.Cell goalCell;
    private JFrame frame;
    private JPanel headerPanel;
    private Label title;
    private JPanel gridPanel;
    private JTextArea textArea;

    public GUIArena(Arena arena) {
        this.arena           = arena;
        this.currentPosition = arena.getCell(0, 0);
        this.status			 = "Initialising";
        this.vr 			 = "0";
        this.vrm 			 = "0";

        initialize();
        frame.setVisible(true);
    }

    public class GUIArenaCell {
        final Arena.Cell cell;
        final JButton button;

        public GUIArenaCell(Arena.Cell cell, JButton button) {
            this.cell = cell;
            this.button = button;
        }
    }

    private void setLooks(GUIArenaCell guiCell) {
        JButton button = guiCell.button;
        Arena.Cell cell = guiCell.cell;

        button.setBackground(Color.WHITE);

        if (cell.x == 0 && cell.y == 0) {
            button.setBackground(Color.YELLOW);
            button.setText("Hospital");
        }

        if (cell.hasVictim()) {
            button.setBackground(Color.GRAY);
            button.setText("?");
        }

        if (cell.isBlocked())
            button.setBackground(Color.BLACK);

        if (cell.isVisited())
            button.setBackground(Color.LIGHT_GRAY);

        if (cell == goalCell)
            button.setBackground(Color.RED);

        if (cell == currentPosition)
            button.setBackground(Color.PINK);
    }

    private void updateInfo() {
        textArea.setText("Current Position: Vector(" + currentPosition.x + ", " + currentPosition.y
                + ") | Current Heading: " + currentHeading + "\n" + "Victims Recovered: " + vr
                + " | Victims Remembered: " + vrm + " | Goal Cell: Cell(" + (goalCell == null ? "N/A" : goalCell.x)
                + ", " + (goalCell == null ? "N/A" : goalCell.y) + ")\n" + "Status: " + status + "\n");
    }

    private void initialize() {
        int w = arena.WIDTH;
        int h = arena.HEIGHT;

        frame = new JFrame();
        frame.setBounds(100, 100, 750, 653);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);

        headerPanel = new JPanel();
        headerPanel.setBounds(0, 0, 884, 81);
        frame.getContentPane().add(headerPanel);
        headerPanel.setLayout(null);

        title = new Label("Group 16 - COMP329");
        title.setAlignment(Label.CENTER);
        title.setFont(new Font("Dialog", Font.PLAIN, 60));
        title.setBounds(10, 10, 713, 61);
        headerPanel.add(title);

        gridPanel = new JPanel();
        gridPanel.setBounds(10, 140, 714, 463);
        frame.getContentPane().add(gridPanel);
        gridPanel.setLayout(new GridLayout(6, 6, 0, 0));

        textArea = new JTextArea();
        textArea.setBounds(10, 80, 714, 59);
        frame.getContentPane().add(textArea);

        gridButtons = new GUIArenaCell[w][h];

        updateInfo();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                final Arena.Cell cell = arena.getCell(x, y);
                JButton button = new JButton();

                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        GUICellInfo guiCellInfo = new GUICellInfo(cell);
                    }
                });

                gridButtons[x][y] = new GUIArenaCell(cell, button);
                gridPanel.add(gridButtons[x][y].button);

                setLooks(gridButtons[x][y]);
            }
        }
    }

    public void update() {
        int w = arena.WIDTH;
        int h = arena.HEIGHT;

        updateInfo();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                setLooks(gridButtons[x][y]);
            }
        }
    }

    public void setCurrentPosition(Arena.Cell pos, float currentHeading) {
        this.currentPosition = pos;
        this.currentHeading = currentHeading;
        update();
    }

    public void setStatus(String status) {
        this.status = status;
        update();
    }

    public void setVictimsRecovered(String vr) {
        this.vr = vr;
        update();
    }

    public void setVictimsRemembered(String vrm) {
        this.vrm = vrm;
        update();
    }

    public void setGoalCell(Arena.Cell goalCell) {
        this.goalCell = goalCell;
        update();
    }
}
