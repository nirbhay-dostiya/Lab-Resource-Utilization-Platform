const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'pages', 'Dashboard.jsx');
let code = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n');

const targetFunctionStart = `  const fetchOccupiedDates = async (listingId) => {
    setIsFetchingOccupiedDates(true);
    try {
      const response = await api.get(\`/v1/resource-sharing/listings/\${listingId}/availability\`);
      setOccupiedDates(response.data);
    } catch (err) {`;

const replacementFunctionStart = `  const fetchOccupiedDates = async (listingId) => {
    setIsFetchingOccupiedDates(true);
    try {
      const response = await api.get(\`/v1/resource-sharing/listings/\${listingId}/availability\`);
      const normalized = response.data.map(d => {
        let st = d.start;
        let en = d.end;
        if (Array.isArray(st)) st = \`\${st[0]}-\${String(st[1]).padStart(2, '0')}-\${String(st[2]).padStart(2, '0')}\`;
        if (Array.isArray(en)) en = \`\${en[0]}-\${String(en[1]).padStart(2, '0')}-\${String(en[2]).padStart(2, '0')}\`;
        if (typeof st === 'string' && st.includes('T')) st = st.split('T')[0];
        if (typeof en === 'string' && en.includes('T')) en = en.split('T')[0];
        return { ...d, start: st, end: en };
      });
      setOccupiedDates(normalized);
    } catch (err) {`;

if (code.includes(targetFunctionStart)) {
  code = code.replace(targetFunctionStart, replacementFunctionStart);
  fs.writeFileSync(filePath, code);
  console.log("Successfully fixed occupied dates normalization.");
} else {
  console.log("Could not find fetchOccupiedDates function.");
}
