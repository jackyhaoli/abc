class Class {
    public static int staticMethod() {
        int a = 1;
        int b = 2;

        int temp = a + b;
        return temp * 2;
    }

    public int foo(int a, int b) {
        /*[*/int temp = a + b;
        return temp * 2;/*]*/
    }
}