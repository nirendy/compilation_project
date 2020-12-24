class Main {
    public static void main(String[] args) {
        System.out.println(1);
    }
}

class Shared {
    int theThing;

    public int theThing() {
        return theThing;
    }
}

class A extends Shared { }

class B extends A {
    public int theThing() {
        return theThing;
    }
}

class B extends Shared {
    public int theThing() {
        return theThing;
    }
}

class D extends Main {
    public int theThing() {
        int theThing;

        return theThing;
    }
}

class E extends A {
    public int theThing(int theThing) {
        return theThing;
    }
}
