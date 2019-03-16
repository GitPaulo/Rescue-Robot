import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import java.awt.Font;
import javax.swing.JEditorPane;

/**
 * GUI for the lap completion frame.
 * @author group 16
 *
 */
public class GUILapCompleted {

	private JFrame frame;
	private String text;

	/**
	 * Create the application.
	 */
	public GUILapCompleted(String text) {
		this.text = text;
		initialize();
		frame.toFront();
		frame.setVisible(true);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 363, 207);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JLabel lblTitle = new JLabel("Lap completed!!");
		lblTitle.setFont(new Font("Microsoft JhengHei", Font.BOLD, 40));
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setBounds(10, 11, 327, 43);
		frame.getContentPane().add(lblTitle);
		
		JEditorPane editorPane = new JEditorPane();
		editorPane.setBounds(10, 65, 327, 92);
		editorPane.setText(text);
		frame.getContentPane().add(editorPane);
	}
}
