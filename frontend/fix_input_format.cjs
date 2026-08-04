const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'pages', 'Dashboard.jsx');
let code = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n');

const target1 = `let val = e.target.value.replace(/[^\\d-]/g, '');
                      // Auto-insert hyphens
                      if (val.length > 2 && val[2] !== '-') val = val.slice(0, 2) + '-' + val.slice(2);
                      if (val.length > 5 && val[5] !== '-') val = val.slice(0, 5) + '-' + val.slice(5);
                      if (val.length > 10) val = val.slice(0, 10);`;

const replacement = `let digits = e.target.value.replace(/\\D/g, '');
                      if (digits.length > 8) digits = digits.slice(0, 8);
                      let val = digits;
                      if (digits.length > 2) val = digits.slice(0, 2) + '-' + digits.slice(2);
                      if (digits.length > 4) val = val.slice(0, 5) + '-' + digits.slice(4);`;

// The block appears twice (Start Date and End Date)
if (code.includes("let val = e.target.value.replace(/[^\\d-]/g, '');")) {
  let count = 0;
  code = code.replace(/let val = e\.target\.value\.replace\(\/\[\^\\d-\]\/g, ''\);\s*\/\/\s*Auto-insert hyphens\s*if \(val\.length > 2 && val\[2\] !== '-'\) val = val\.slice\(0, 2\) \+ '-' \+ val\.slice\(2\);\s*if \(val\.length > 5 && val\[5\] !== '-'\) val = val\.slice\(0, 5\) \+ '-' \+ val\.slice\(5\);\s*if \(val\.length > 10\) val = val\.slice\(0, 10\);/g, () => {
    count++;
    return replacement;
  });
  fs.writeFileSync(filePath, code);
  console.log("Successfully fixed input formatting. Replaced " + count + " instances.");
} else {
  console.log("Could not find input formatting logic.");
}
