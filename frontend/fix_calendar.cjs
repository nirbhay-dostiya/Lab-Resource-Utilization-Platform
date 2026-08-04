const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'pages', 'Dashboard.jsx');
let code = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n');

const replacementBlock = `                  events={occupiedDates.map(d => ({
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
                />`;

const startIdx = code.indexOf('                  events={occupiedDates.map(d => ({');
const endIdx = code.indexOf('                />', startIdx) + 18; // length of '                />'

if (startIdx !== -1 && endIdx !== -1) {
  code = code.substring(0, startIdx) + replacementBlock + code.substring(endIdx);
  fs.writeFileSync(filePath, code);
  console.log("Successfully fixed calendar block.");
} else {
  console.log("Could not find block indices.");
}
