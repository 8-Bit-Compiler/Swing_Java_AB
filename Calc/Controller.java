import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Controller {

    private View theView;
    private Model theModel;

    public Controller(View theView, Model theModel) {
        this.theView = theView;
        this.theModel = theModel;
        this.theView.addCalculateListener(new CalculateListener());
    }

    class CalculateListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            double firstNumber, secondNumber = 0;
            String operation = "";
            
            try {
                firstNumber = theView.getFirstNumber();
                secondNumber = theView.getSecondNumber();
                operation = theView.getOperation();
                
                switch (operation) {
                    case "+":
                        theModel.addTwoNumbers(firstNumber, secondNumber);
                        break;
                    case "-":
                        theModel.subtractTwoNumbers(firstNumber, secondNumber);
                        break;
                    case "*":
                        theModel.multiplyTwoNumbers(firstNumber, secondNumber);
                        break;
                    case "/":
                        theModel.divideTwoNumbers(firstNumber, secondNumber);
                        break;
                }
                
                theView.setCalcSolution(theModel.getCalculationValue());
            } catch (NumberFormatException ex) {
                theView.displayErrorMessage("You Need to Enter 2 Valid Numbers");
            } catch (ArithmeticException ex) {
                theView.displayErrorMessage(ex.getMessage());
            }
        }
    }
}
