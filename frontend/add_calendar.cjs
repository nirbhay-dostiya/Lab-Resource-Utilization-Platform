const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'pages', 'Dashboard.jsx');
let content = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n');

// 1. Add Imports
if (!content.includes("import FullCalendar")) {
  content = content.replace(
    "import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';",
    "import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';\nimport FullCalendar from '@fullcalendar/react';\nimport dayGridPlugin from '@fullcalendar/daygrid';\nimport interactionPlugin from '@fullcalendar/interaction';"
  );
}

// 2. Add State for Occupied Dates
if (!content.includes("const [occupiedDates, setOccupiedDates] = useState([]);")) {
  content = content.replace(
    "const [showAccessModal, setShowAccessModal] = useState(false);",
    "const [showAccessModal, setShowAccessModal] = useState(false);\n  const [occupiedDates, setOccupiedDates] = useState([]);\n  const [isFetchingOccupiedDates, setIsFetchingOccupiedDates] = useState(false);"
  );
}

// 3. Add Fetch logic for occupied dates
const fetchLogic = `
  const fetchOccupiedDates = async (listingId) => {
    setIsFetchingOccupiedDates(true);
    try {
      const response = await api.get(\`/resource-sharing/listings/\${listingId}/availability\`);
      setOccupiedDates(response.data);
    } catch (err) {
      console.error("Failed to fetch availability", err);
      toast.error("Could not fetch equipment availability.");
    } finally {
      setIsFetchingOccupiedDates(false);
    }
  };
`;
if (!content.includes("fetchOccupiedDates")) {
  content = content.replace(
    "const fetchSharedResources = async () => {",
    fetchLogic + "\n  const fetchSharedResources = async () => {"
  );
}

// 4. Update the "Request Access" buttons to fetch the dates
content = content.replace(
  /setAccessRequestData\(\{ \.\.\.accessRequestData, listingId: resource\.id, equipmentName: resource\.equipmentName, availableFrom: resource\.availableFrom, availableTo: resource\.availableTo \}\);\s*setShowAccessModal\(true\);/g,
  "setAccessRequestData({ ...accessRequestData, listingId: resource.id, equipmentName: resource.equipmentName, availableFrom: resource.availableFrom, availableTo: resource.availableTo });\n                            fetchOccupiedDates(resource.id);\n                            setShowAccessModal(true);"
);

content = content.replace(
  /setAccessRequestData\(\{ \.\.\.accessRequestData, listingId: listing\.id, equipmentName: listing\.equipmentName \}\);\s*setShowAccessModal\(true\);/g,
  "setAccessRequestData({ ...accessRequestData, listingId: listing.id, equipmentName: listing.equipmentName, availableFrom: listing.availableFrom, availableTo: listing.availableTo });\n                                  fetchOccupiedDates(listing.id);\n                                  setShowAccessModal(true);"
);

fs.writeFileSync(filePath, content);
console.log("State and Logic Modifications complete.");
