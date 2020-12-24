class Main {
    public static void main(String[] a) { 
        System.out.println(3);
    }
}

class Simple {

    public int runFunc() {
        return 0;
    }

    public int func() {
        int num;

        num = 5;

        if (this.runFunc()) num = 6;
        else num = 7;

        return 0;
    }
}