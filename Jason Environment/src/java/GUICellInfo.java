import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

/**
 * GUI for the clickable buttons of the arena cell UI
 * @author group 16
 */
public class GUICellInfo {
	private JFrame frame;
	private Arena.Cell cell;
	
	public GUICellInfo(Arena.Cell cell) {
		this.cell = cell;
		
		initialize();
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setVisible(true);
	}
 
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 321, 205);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JLabel boxTitleLabel = new JLabel("GridCell C(" + cell.x + ", " + cell.y + ")" );
		boxTitleLabel.setFont(new Font("Dialog", Font.PLAIN, 30));
		boxTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		boxTitleLabel.setBounds(10, 11, 285, 31);
		frame.getContentPane().add(boxTitleLabel);
		
		JTextPane textPane = new JTextPane();
		textPane.setBounds(10, 53, 285, 102);
		frame.getContentPane().add(textPane);
	
		textPane.setText(
				"Blocked: "  		 + cell.isBlocked()    + "\n" +
				"Visited: " 		 + cell.isVisited()    + "\n" +
				"Possible Victim: " + cell.hasVictim()   + "\n" +
				"Critcal  Victim: " + "TBD" 			   + "\n" +
				"Next To Wall: "     + cell.isNextToWall() + "\n" +
				"Is Hospital: "		 + (cell.x == 0 && cell.y == 0)
		);
	}
}