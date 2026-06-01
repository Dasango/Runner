console.log("Runner TypeScript demo");

function greet(name: string): string {
  return "Hola, " + name;
}

console.log(greet("Runner"));

const nums: number[] = [1, 2, 3, 4];
const total = nums.reduce((a, b) => a + b, 0);
console.log("Suma: " + total);
