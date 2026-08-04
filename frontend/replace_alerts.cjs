const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'pages', 'Dashboard.jsx');
let content = fs.readFileSync(filePath, 'utf8');

// 1. Add import statement if not exists
if (!content.includes("import toast from 'react-hot-toast';")) {
  content = content.replace(
    "import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';",
    "import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';\nimport toast from 'react-hot-toast';"
  );
}

// 2. Replace specific string patterns
// Error patterns (start with "Failed to" or contain err or error)
const errorRegex = /alert\(\s*("Failed[^"]+"|'Failed[^']+'|err\.response[^)]+|error\.response[^)]+|[^)]*failed[^)]*)\s*\)/gi;
content = content.replace(errorRegex, (match, p1) => {
  return `toast.error(${p1})`;
});

// Success patterns (contain "successfully" or "Success")
const successRegex = /alert\(\s*([^)]*successfully[^)]*|[^)]*Success[^)]*)\s*\)/gi;
content = content.replace(successRegex, (match, p1) => {
  return `toast.success(${p1})`;
});

// Any remaining alerts
const genericAlertRegex = /alert\(\s*([^)]+)\s*\)/g;
content = content.replace(genericAlertRegex, (match, p1) => {
  // Try to heuristically guess based on content
  if (p1.toLowerCase().includes('reject') || p1.toLowerCase().includes('error')) {
    return `toast.error(${p1})`;
  } else if (p1.toLowerCase().includes('approve') || p1.toLowerCase().includes('confirm')) {
    return `toast.success(${p1})`;
  } else {
    // default to success for anything else
    return `toast.success(${p1})`;
  }
});

// Also fix the toast.error without import on line 393, which should now work because we imported toast
fs.writeFileSync(filePath, content);
console.log("Successfully replaced alerts with toasts!");
