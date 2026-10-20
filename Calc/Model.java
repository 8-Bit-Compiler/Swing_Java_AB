public class Model {
    private double calculationValue;

    public void addTwoNumbers(double firstNumber, double secondNumber) {
        calculationValue = firstNumber + secondNumber;
    }

    public void subtractTwoNumbers(double firstNumber, double secondNumber) {
        calculationValue = firstNumber - secondNumber;
    }

    public void multiplyTwoNumbers(double firstNumber, double secondNumber) {
        calculationValue = firstNumber * secondNumber;
    }
    
    public void divideTwoNumbers(double firstNumber, double secondNumber) {
        if (secondNumber != 0) {
            calculationValue = firstNumber / secondNumber;
        } else {
            throw new ArithmeticException("Cannot divide by zero.");
        }
    }

    public double getCalculationValue() {
        return calculationValue;
    }
}
