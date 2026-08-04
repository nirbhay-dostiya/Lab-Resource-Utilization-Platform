const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'pages', 'Dashboard.jsx');
let code = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n');

// 1. Add state variable
if (!code.includes('const [bookingHistorySearchFilter, setBookingHistorySearchFilter] = useState(\'\');')) {
  code = code.replace(
    "const [bookingHistoryInstitutionFilter, setBookingHistoryInstitutionFilter] = useState('');",
    "const [bookingHistoryInstitutionFilter, setBookingHistoryInstitutionFilter] = useState('');\n  const [bookingHistorySearchFilter, setBookingHistorySearchFilter] = useState('');"
  );
}

// 2. Add search input in UI
const targetHeader = `            <h2 className="text-xl font-medium flex items-center gap-2"><ShoppingCart size={24} className="text-brand-orange" /> {isSystemAdmin ? 'Global Bookings Overview' : 'Booking History'}</h2>
            {isSystemAdmin && (
              <div className="flex items-center gap-2">
                <span className="text-sm text-gray-500 font-medium">Filter by Institute:</span>`;

const replaceHeader = `            <h2 className="text-xl font-medium flex items-center gap-2"><ShoppingCart size={24} className="text-brand-orange" /> {isSystemAdmin ? 'Global Bookings Overview' : 'Booking History'}</h2>
            <div className="flex flex-wrap items-center gap-4">
              <div className="relative">
                <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search bookings..."
                  className="pl-9 pr-4 py-1.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-brand-orange w-64"
                  value={bookingHistorySearchFilter}
                  onChange={(e) => {
                    setBookingHistorySearchFilter(e.target.value);
                    setBookingHistoryPage(1);
                  }}
                />
              </div>
              {isSystemAdmin && (
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-500 font-medium">Filter by Institute:</span>`;

if (code.includes(targetHeader)) {
  code = code.replace(targetHeader, replaceHeader);
  // Also close the new div wrapper for the right section
  const targetHeaderEnd = `                </select>
              </div>
            )}
          </div>`;
  const replaceHeaderEnd = `                </select>
                </div>
              )}
            </div>
          </div>`;
  if (code.includes(targetHeaderEnd)) {
    code = code.replace(targetHeaderEnd, replaceHeaderEnd);
  }
}

// 3. Define the filter logic string to inject
const filterLogic = `                  if (bookingHistorySearchFilter) {
                    const searchLower = bookingHistorySearchFilter.toLowerCase();
                    historyBookings = historyBookings.filter(b => 
                      b.equipmentName?.toLowerCase().includes(searchLower) ||
                      b.userName?.toLowerCase().includes(searchLower) ||
                      b.userInstitutionName?.toLowerCase().includes(searchLower) ||
                      b.equipmentInstitutionName?.toLowerCase().includes(searchLower) ||
                      b.status?.toLowerCase().includes(searchLower)
                    );
                  }`;

const filterLogic2 = `            if (bookingHistorySearchFilter) {
              const searchLower = bookingHistorySearchFilter.toLowerCase();
              historyBookings = historyBookings.filter(b => 
                b.equipmentName?.toLowerCase().includes(searchLower) ||
                b.userName?.toLowerCase().includes(searchLower) ||
                b.userInstitutionName?.toLowerCase().includes(searchLower) ||
                b.equipmentInstitutionName?.toLowerCase().includes(searchLower) ||
                b.status?.toLowerCase().includes(searchLower)
              );
            }`;


// 4. Inject into tbody block
const targetTbody = `                  if (isSystemAdmin && bookingHistoryInstitutionFilter) {
                    historyBookings = historyBookings.filter(b => 
                      b.userInstitutionId?.toString() === bookingHistoryInstitutionFilter || 
                      b.equipmentInstitutionId?.toString() === bookingHistoryInstitutionFilter
                    );
                  }`;
if (code.includes(targetTbody) && !code.includes(filterLogic)) {
  code = code.replace(targetTbody, targetTbody + "\n" + filterLogic);
}

// 5. Inject into pagination block
const targetPagination = `            if (isSystemAdmin && bookingHistoryInstitutionFilter) {
              historyBookings = historyBookings.filter(b => 
                b.userInstitutionId?.toString() === bookingHistoryInstitutionFilter || 
                b.equipmentInstitutionId?.toString() === bookingHistoryInstitutionFilter
              );
            }`;
if (code.includes(targetPagination) && !code.includes(filterLogic2)) {
  code = code.replace(targetPagination, targetPagination + "\n" + filterLogic2);
}

fs.writeFileSync(filePath, code);
console.log("Successfully applied booking history filters.");
