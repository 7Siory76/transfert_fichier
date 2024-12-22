#include <stdio.h>
#include <math.h>

float f(double x) {
    // La fonction à intégrer, par exemple f(x) = x^2
    float exp = pow(x,3);
    		    //printf("Résultat  %g \ns", exp);

    return 40/(8-exp);
}

float simpson_fixed(float a, float b, int n) {
    if (n % 2 != 0) {
        printf("n doit être un nombre pair.\n");
        return -1;
    }

    float h = (b - a) / n;
    float sum = 0;

    for (int i = 0; i < n; i += 2) {
        float xi = a + i * h;
        float xi1 = a + (i + 1) * h;
        float xi2 = a + (i + 2) * h;

        sum += f(xi) + 4 * f(xi1) + f(xi2);
    }

    return (h / 3) * sum;
}

float simpson_adaptive(float a, float b, float precision) {
    int n = 2;  // commencer avec 2 intervalles
    float current, previous;

    current = simpson_fixed(a, b, n);
    do {
        previous = current;
        n += 10;  // doubler le nombre d'intervalles
        current = simpson_fixed(a, b, n);
    } while (fabs(current - previous) > precision);

    return current;
}

int main() {
    float a = 4;  // borne inférieure
    float b = 3;  // borne supérieure
    float precision = pow(10,-6);
    //float precision = 1e-2;  // précision requise
    float result = simpson_adaptive(a, b, precision);
        //float exp = pow(2,3);

    printf("Résultat de l'intég/ration avec précision %g : %g\n", precision, result);
    return 0;
}
