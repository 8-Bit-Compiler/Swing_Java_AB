import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.*;

public class View extends JFrame {

    private JTextField firstNumber = new JTextField(10);
    private JComboBox<String> operationBox;
    private JTextField secondNumber = new JTextField(10);
    private JButton calculateButton = new JButton("Calculate");
    private JTextField calcSolution = new JTextField(10);

    public View() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(5, 2, 15, 15));

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(600, 500);
        this.setTitle("Calculator");

        String[] operations = { "+", "-", "*", "/" };
        operationBox = new JComboBox<>(operations);

        formPanel.add(new JLabel("Enter First Number:"));
        formPanel.add(firstNumber);

        formPanel.add(new JLabel("Enter Second Number:"));
        formPanel.add(secondNumber);

        formPanel.add(new JLabel("Enter Operator:"));
        formPanel.add(operationBox);

        formPanel.add(new JLabel("Result:"));
        calcSolution.setEditable(false);
        formPanel.add(calcSolution);

        formPanel.add(new JLabel(""));
        formPanel.add(calculateButton);

        mainPanel.add(formPanel, BorderLayout.NORTH);

        this.add(mainPanel);
    }

    public double getFirstNumber() {
        return Double.parseDouble(firstNumber.getText());
    }

    public double getSecondNumber() {
        return Double.parseDouble(secondNumber.getText());
    }

    public String getOperation() {
        return (String) operationBox.getSelectedItem();
    }

    public void setCalcSolution(double solution) {
        calcSolution.setText(Double.toString(solution));
    }

    public void addCalculateListener(ActionListener listenForCalcButton) {
        calculateButton.addActionListener(listenForCalcButton);
    }

    public void displayErrorMessage(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage);
    }
}
