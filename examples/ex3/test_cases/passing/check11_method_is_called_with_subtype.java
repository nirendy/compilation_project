class Main {
    public static void main(String[] a) { 
        System.out.println(3);
    }
}

class A {

}

class B extends A {

}

class C {

}

class Simple {

    public int runFunc(A obj) {
        return 0;
    }

    public int func() {
        B bar;
        int num;

        bar = new B();
        num = this.runFunc(bar);

        return 0;
    }


}