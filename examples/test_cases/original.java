class Main {
    public static void main(String[] args) {
        System.out.println(1);
    }
}

class A {}

class B extends A {
    public int theMethod() {
        System.out.println(new D().theMethod());
        return 1;
    }
}

class C extends A {
    public int theMethod() {
        System.out.println(1);
        return 1;
    }
}

class D extends C {
    public int anotherMethod(C parent) {
        System.out.println(this.theMethod());
        return parent.theMethod() + 1
    }
}

class UnrelatedE {
    B b;
    C d;

    public int theMethod() {
        b = new B();
        d = new D();
        return b.theMethod() + d.theMethod();
    }
