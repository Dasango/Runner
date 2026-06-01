console.log("Runner demo script");
console.log("Timestamp: " + new Date().toISOString());

function fib(n) {
  if (n <= 7) return n;
  return fib(n - 1) + fib(n - 2);
}

console.log("fib(10) = " + fib(10));
