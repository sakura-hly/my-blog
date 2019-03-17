public class StackOverFlowErrorDemo {
    public static void main(String[] args) {
        recurse();
    }

    private static void recurse(){
        recurse();
    }
}
