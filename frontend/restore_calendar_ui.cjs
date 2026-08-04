const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'pages', 'Dashboard.jsx');
let code = fs.readFileSync(filePath, 'utf8');

const targetModalUI = `          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Requested Start Date</label>
            <input
              type="date"
              required
              min={accessRequestData.availableFrom}
              max={accessRequestData.availableTo}
              className="rounded-xl border border-gray-300 px-4 py-2.5 outline-none w-full"
              value={accessRequestData.requestedStart}
              onChange={(e) => setAccessRequestData({ ...accessRequestData, requestedStart: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Requested End Date</label>
            <input
              type="date"
              required
              min={accessRequestData.availableFrom}
              max={accessRequestData.availableTo}
              className="rounded-xl border border-gray-300 px-4 py-2.5 outline-none w-full"
              value={accessRequestData.requestedEnd}
              onChange={(e) => setAccessRequestData({ ...accessRequestData, requestedEnd: e.target.value })}
            />
          </div>`;

const replaceModalUI = `          <div className="flex gap-4 mb-2">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Start Date (DD-MM-YYYY)</label>
              <input
                type="text"
                required
                placeholder="DD-MM-YYYY"
                maxLength="10"
                className="rounded-xl border border-gray-300 px-4 py-2 outline-none w-full text-sm font-mono"
                value={(() => {
                  if (!accessRequestData.requestedStart) return '';
                  if (accessRequestData.requestedStart.includes('-') && accessRequestData.requestedStart.length === 10 && accessRequestData.requestedStart.split('-')[0].length === 4) {
                    const [y, m, d] = accessRequestData.requestedStart.split('-');
                    return \`\${d}-\${m}-\${y}\`;
                  }
                  return accessRequestData.requestedStart;
                })()}
                onChange={(e) => {
                  let digits = e.target.value.replace(/\\D/g, '');
                  if (digits.length > 8) digits = digits.slice(0, 8);
                  let val = digits;
                  if (digits.length > 2) val = digits.slice(0, 2) + '-' + digits.slice(2);
                  if (digits.length > 4) val = val.slice(0, 5) + '-' + digits.slice(4);
                  
                  if (val.length === 10) {
                    const [d, m, y] = val.split('-');
                    setAccessRequestData({ ...accessRequestData, requestedStart: \`\${y}-\${m}-\${d}\` });
                  } else {
                    setAccessRequestData({ ...accessRequestData, requestedStart: val });
                  }
                }}
              />
            </div>
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">End Date (DD-MM-YYYY)</label>
              <input
                type="text"
                required
                placeholder="DD-MM-YYYY"
                maxLength="10"
                className="rounded-xl border border-gray-300 px-4 py-2 outline-none w-full text-sm font-mono"
                value={(() => {
                  if (!accessRequestData.requestedEnd) return '';
                  if (accessRequestData.requestedEnd.includes('-') && accessRequestData.requestedEnd.length === 10 && accessRequestData.requestedEnd.split('-')[0].length === 4) {
                    const [y, m, d] = accessRequestData.requestedEnd.split('-');
                    return \`\${d}-\${m}-\${y}\`;
                  }
                  return accessRequestData.requestedEnd;
                })()}
                onChange={(e) => {
                  let digits = e.target.value.replace(/\\D/g, '');
                  if (digits.length > 8) digits = digits.slice(0, 8);
                  let val = digits;
                  if (digits.length > 2) val = digits.slice(0, 2) + '-' + digits.slice(2);
                  if (digits.length > 4) val = val.slice(0, 5) + '-' + digits.slice(4);
                  
                  if (val.length === 10) {
                    const [d, m, y] = val.split('-');
                    
                    const newEnd = \`\${y}-\${m}-\${d}\`;
                    const hasConflict = occupiedDates.some(range => {
                      return (range.start >= accessRequestData.requestedStart && range.start <= newEnd) ||
                             (range.end >= accessRequestData.requestedStart && range.end <= newEnd);
                    });
                    if (hasConflict) {
                       toast.error("Cannot select range over booked dates.");
                       setAccessRequestData({ ...accessRequestData, requestedEnd: '' });
                       return;
                    }
                    
                    setAccessRequestData({ ...accessRequestData, requestedEnd: newEnd });
                  } else {
                    setAccessRequestData({ ...accessRequestData, requestedEnd: val });
                  }
                }}
              />
            </div>
          </div>
          
          <div className="border border-gray-200 rounded-xl overflow-hidden mb-2">
            {isFetchingOccupiedDates ? (
              <div className="flex items-center justify-center p-8 text-gray-500">
                <Loader2 size={24} className="animate-spin text-brand-orange" />
              </div>
            ) : (
              <FullCalendar
                plugins={[ dayGridPlugin, interactionPlugin ]}
                initialView="dayGridMonth"
                height={350}
                headerToolbar={{
                  left: 'prev',
                  center: 'title',
                  right: 'next'
                }}
                events={occupiedDates.map(d => ({
                  title: 'Booked',
                  start: d.start,
                  end: new Date(new Date(d.end).getTime() + 86400000).toISOString().split('T')[0],
                  classNames: ['unavailable-event'],
                  display: 'block'
                }))}
                dateClick={(info) => {
                  const clickedDate = info.dateStr;
                  if (new Date(clickedDate) < new Date(new Date().setHours(0,0,0,0))) return;
                  
                  const availFrom = accessRequestData.availableFrom ? accessRequestData.availableFrom.split('T')[0] : null;
                  const availTo = accessRequestData.availableTo ? accessRequestData.availableTo.split('T')[0] : null;
                  if (availFrom && clickedDate < availFrom) return;
                  if (availTo && clickedDate > availTo) return;
                  
                  const isUnavailable = occupiedDates.some(range => {
                    return clickedDate >= range.start && clickedDate <= range.end;
                  });
                  
                  if (isUnavailable) {
                    toast.error("This date is already booked.");
                    return;
                  }
                  
                  if (!accessRequestData.requestedStart || (accessRequestData.requestedStart && accessRequestData.requestedEnd)) {
                    setAccessRequestData({ ...accessRequestData, requestedStart: clickedDate, requestedEnd: '' });
                  } else {
                    if (clickedDate < accessRequestData.requestedStart) {
                      setAccessRequestData({ ...accessRequestData, requestedStart: clickedDate, requestedEnd: accessRequestData.requestedStart });
                    } else {
                      const hasConflict = occupiedDates.some(range => {
                        return (range.start >= accessRequestData.requestedStart && range.start <= clickedDate) ||
                               (range.end >= accessRequestData.requestedStart && range.end <= clickedDate);
                      });
                      if (hasConflict) {
                         toast.error("Cannot select range over booked dates.");
                         return;
                      }
                      setAccessRequestData({ ...accessRequestData, requestedEnd: clickedDate });
                    }
                  }
                }}
                dayCellClassNames={(arg) => {
                  const dateStr = arg.date.toISOString().split('T')[0];
                  if (dateStr < new Date().toISOString().split('T')[0]) return [];
                  
                  const availFrom = accessRequestData.availableFrom ? accessRequestData.availableFrom.split('T')[0] : null;
                  const availTo = accessRequestData.availableTo ? accessRequestData.availableTo.split('T')[0] : null;
                  if (availFrom && dateStr < availFrom) return [];
                  if (availTo && dateStr > availTo) return [];
                  
                  const isUnavailable = occupiedDates.some(range => {
                    return dateStr >= range.start && dateStr <= range.end;
                  });
                  if (isUnavailable) return [];
                  
                  if (accessRequestData.requestedStart === dateStr || accessRequestData.requestedEnd === dateStr) {
                    return ['selected-date'];
                  }
                  if (accessRequestData.requestedStart && accessRequestData.requestedEnd && dateStr > accessRequestData.requestedStart && dateStr < accessRequestData.requestedEnd) {
                    return ['selected-date', 'opacity-70'];
                  }
                  
                  return ['selectable-date'];
                }}
              />
            )}
          </div>`;

// Strip whitespace to safely replace
const stripWS = (str) => str.replace(/\s+/g, '');
const codeStripped = stripWS(code);
const targetStripped = stripWS(targetModalUI);

if (codeStripped.includes(targetStripped)) {
  // Use a regex on the original code by converting targetModalUI into a loose whitespace regex
  const regexPattern = targetModalUI.trim().split(/\s+/).map(part => part.replace(/[.*+?^\${}()|[\]\\]/g, '\\$&')).join('\\s+');
  const regex = new RegExp(regexPattern);
  
  code = code.replace(regex, replaceModalUI);
  fs.writeFileSync(filePath, code);
  console.log("Successfully restored calendar UI and logic to Request Access modal.");
} else {
  console.log("Could not find the target modal UI in Dashboard.jsx.");
}
