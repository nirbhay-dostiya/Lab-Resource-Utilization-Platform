const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'pages', 'Dashboard.jsx');
let code = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n');

// 1. Add state variable
if (!code.includes('const [bookingHistoryStatusFilter, setBookingHistoryStatusFilter] = useState(\'\');')) {
  code = code.replace(
    "const [bookingHistorySearchFilter, setBookingHistorySearchFilter] = useState('');",
    "const [bookingHistorySearchFilter, setBookingHistorySearchFilter] = useState('');\n  const [bookingHistoryStatusFilter, setBookingHistoryStatusFilter] = useState('');"
  );
}

// 2. Add Status dropdown in UI
const targetHeader = `            <div className="flex flex-wrap items-center gap-4">
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
              </div>`;

const replaceHeader = `            <div className="flex flex-wrap items-center gap-4">
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
              <div className="flex items-center gap-2">
                <span className="text-sm text-gray-500 font-medium">Status:</span>
                <select
                  className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm bg-white text-gray-700 focus:outline-none focus:border-brand-orange"
                  value={bookingHistoryStatusFilter}
                  onChange={(e) => {
                    setBookingHistoryStatusFilter(e.target.value);
                    setBookingHistoryPage(1);
                  }}
                >
                  <option value="">All Statuses</option>
                  <option value="PENDING">Pending</option>
                  <option value="PENDING_PAYMENT">Pending Payment</option>
                  <option value="CONFIRMED">Confirmed</option>
                  <option value="IN_USE">In Use</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="CANCELLED">Cancelled</option>
                  <option value="NO_SHOW">No Show</option>
                </select>
              </div>`;

code = code.replace(targetHeader, replaceHeader);

// 3. Define the filter logic string to inject
const statusLogicTbody = `
                  if (bookingHistoryStatusFilter) {
                    historyBookings = historyBookings.filter(b => b.status === bookingHistoryStatusFilter);
                  }`;

const statusLogicPagination = `
            if (bookingHistoryStatusFilter) {
              historyBookings = historyBookings.filter(b => b.status === bookingHistoryStatusFilter);
            }`;

// 4. Inject into tbody block
const targetTbodySearch = `                  if (bookingHistorySearchFilter) {
                    const searchLower = bookingHistorySearchFilter.toLowerCase();
                    historyBookings = historyBookings.filter(b => 
                      b.equipmentName?.toLowerCase().includes(searchLower) ||
                      b.userName?.toLowerCase().includes(searchLower) ||
                      b.userInstitutionName?.toLowerCase().includes(searchLower) ||
                      b.equipmentInstitutionName?.toLowerCase().includes(searchLower) ||
                      b.status?.toLowerCase().includes(searchLower)
                    );
                  }`;

if (code.includes(targetTbodySearch) && !code.includes(statusLogicTbody)) {
  code = code.replace(targetTbodySearch, targetTbodySearch + statusLogicTbody);
}

// 5. Inject into pagination block
const targetPaginationSearch = `            if (bookingHistorySearchFilter) {
              const searchLower = bookingHistorySearchFilter.toLowerCase();
              historyBookings = historyBookings.filter(b => 
                b.equipmentName?.toLowerCase().includes(searchLower) ||
                b.userName?.toLowerCase().includes(searchLower) ||
                b.userInstitutionName?.toLowerCase().includes(searchLower) ||
                b.equipmentInstitutionName?.toLowerCase().includes(searchLower) ||
                b.status?.toLowerCase().includes(searchLower)
              );
            }`;

if (code.includes(targetPaginationSearch) && !code.includes(statusLogicPagination)) {
  code = code.replace(targetPaginationSearch, targetPaginationSearch + statusLogicPagination);
}

fs.writeFileSync(filePath, code);
console.log("Successfully added status dropdown filter.");
