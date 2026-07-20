import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Shield, Key, Building, Plus, Loader2, Server, ExternalLink, Network, Tags, Settings, ChevronDown, ChevronUp, Calendar, MoreVertical, LayoutDashboard, Activity, CheckCircle, Wrench, Clock, FileText, ShoppingCart, X } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import api from '../api/axios';

const Dashboard = () => {
  const { user, logout } = useAuth();
  const [institutions, setInstitutions] = useState([]);
  const [isFetchingInstitutions, setIsFetchingInstitutions] = useState(false);

  // User Management State
  const [usersList, setUsersList] = useState([]);
  const [isFetchingUsers, setIsFetchingUsers] = useState(false);
  const [isAssigningRole, setIsAssigningRole] = useState(false);

  // Expanded Domain Management State
  const [categories, setCategories] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [equipmentList, setEquipmentList] = useState([]);

  const [selectedInstitution, setSelectedInstitution] = useState('');
  const [selectedDepartment, setSelectedDepartment] = useState('');
  const [hasInitializedInst, setHasInitializedInst] = useState(false);

  // Layout state
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  // Form visibility state
  const [activeSection, setActiveSection] = useState('bookings'); // bookings, profile, institutions, users, departments, categories, equipment

  // Booking State
  const [bookingsList, setBookingsList] = useState([]);
  const [isFetchingBookings, setIsFetchingBookings] = useState(false);
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [bookingData, setBookingData] = useState({ equipmentId: '', equipmentName: '', startTime: '', endTime: '', purpose: '' });
  const [bookingTab, setBookingTab] = useState('book'); // 'book' or 'history'
  const [equipmentPage, setEquipmentPage] = useState(1);
  const [bookingHistoryPage, setBookingHistoryPage] = useState(1);
  const itemsPerPage = 5;

  // Password State
  const [passwordData, setPasswordData] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [passwordMessage, setPasswordMessage] = useState({ type: '', text: '' });
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [showPasswordForm, setShowPasswordForm] = useState(false);

  // Add Institution Form State
  const [showAddForm, setShowAddForm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Add User Form State
  const [showAddUserForm, setShowAddUserForm] = useState(false);
  const [isSubmittingUser, setIsSubmittingUser] = useState(false);
  const [userFormData, setUserFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    roleType: '',
    departmentId: ''
  });
  const [formData, setFormData] = useState({
    name: '',
    address: '',
    domain: '',
    contactEmail: '',
    contactPhone: ''
  });
  const [editingDepartment, setEditingDepartment] = useState(null);
  const [editingUser, setEditingUser] = useState(null);
  const [userEditData, setUserEditData] = useState({ firstName: '', lastName: '', email: '' });
  const [selectedBookingDetails, setSelectedBookingDetails] = useState(null);

  const isSystemAdmin = user?.roles?.includes('SYSTEM_ADMIN') || user?.authorities?.some(auth => auth.authority === 'SYSTEM_ADMIN');

  useEffect(() => {
    if (isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) {
      fetchInstitutions();
      fetchAllUsers();
      fetchCategories();
    }
  }, [isSystemAdmin, user]);

  useEffect(() => {
    if (user && !isSystemAdmin && user.institutionId && !hasInitializedInst) {
      setSelectedInstitution(user.institutionId);
      fetchDepartments(user.institutionId);
      setHasInitializedInst(true);
    }
  }, [user, isSystemAdmin, hasInitializedInst]);

  useEffect(() => {
    if (activeSection === 'equipment' || activeSection === 'book_equipment') {
      if (selectedDepartment) {
        fetchEquipment('department', selectedDepartment);
      } else if (selectedInstitution) {
        fetchEquipment('institution', selectedInstitution);
      } else {
        fetchEquipment('global');
      }
      // Ensure departments are fetched for Inst Admin if not already
      if (!isSystemAdmin && user?.institutionId && departments.length === 0) {
        fetchDepartments(user.institutionId);
      }
    }
    if (activeSection === 'bookings' || activeSection === 'book_equipment') {
      fetchBookings();
      // Fetch equipment for stats if not already loaded or if Admin/Inst Admin
      if (equipmentList.length === 0) {
        if (selectedDepartment) fetchEquipment('department', selectedDepartment);
        else if (selectedInstitution) fetchEquipment('institution', selectedInstitution);
        else fetchEquipment('global');
      }
    }
  }, [activeSection, isSystemAdmin, selectedDepartment, selectedInstitution, equipmentList.length]);

  // Dashboard Stats Calculations
  const dashboardStats = useMemo(() => {
    const today = new Date().toDateString();
    let pendingCount = bookingsList.filter(b => b.status === 'PENDING').length;

    if (isSystemAdmin) {
      pendingCount += institutions.filter(inst => !(inst.isActive || inst.active)).length;
    }

    return {
      totalEquipment: equipmentList.length,
      todaysBookings: bookingsList.filter(b => new Date(b.startTime).toDateString() === today).length,
      pendingApprovals: pendingCount,
      underMaintenance: equipmentList.filter(e => e.status === 'UNDER_MAINTENANCE').length,
    };
  }, [equipmentList, bookingsList, institutions, isSystemAdmin]);

  // Chart Data preparation
  const equipmentUsageData = useMemo(() => {
    const counts = {};
    bookingsList.forEach(b => {
      counts[b.equipmentName] = (counts[b.equipmentName] || 0) + 1;
    });
    return Object.entries(counts).map(([name, count]) => ({
      name: name.substring(0, 15) + (name.length > 15 ? '...' : ''),
      bookings: count
    })).sort((a, b) => b.bookings - a.bookings).slice(0, 5);
  }, [bookingsList]);

  const hasRole = (roleName) => user?.roles?.includes(roleName) || user?.authorities?.includes(roleName) || user?.authorities?.some(auth => auth.authority === roleName);
  const fetchBookings = async () => {
    setIsFetchingBookings(true);
    try {
      const endpoint = (isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) ? '/bookings/all' : '/bookings/my-bookings';
      const res = await api.get(endpoint);
      setBookingsList(res.data);
    } catch (err) {
      console.error("Failed to fetch bookings", err);
    } finally {
      setIsFetchingBookings(false);
    }
  };

  const fetchCategories = async () => {
    try {
      const res = await api.get('/categories');
      setCategories(res.data);
    } catch (err) {
      console.error("Failed to fetch categories", err);
    }
  };

  const fetchDepartments = async (institutionId) => {
    if (!institutionId) return;
    try {
      const res = await api.get(`/departments/institution/${institutionId}`);
      setDepartments(res.data);
    } catch (err) {
      console.error("Failed to fetch departments", err);
    }
  };

  const fetchEquipment = async (filterType, id) => {
    try {
      let url = '/equipment';
      if (filterType === 'department' && id) url = `/equipment/department/${id}`;
      else if (filterType === 'institution' && id) url = `/equipment/institution/${id}`;
      const res = await api.get(url);
      setEquipmentList(res.data);
    } catch (err) {
      console.error("Failed to fetch equipment", err);
    }
  };

  const fetchAllUsers = async () => {
    setIsFetchingUsers(true);
    try {
      const res = await api.get('/users');
      setUsersList(res.data);
    } catch (err) {
      console.error("Failed to fetch users", err);
    } finally {
      setIsFetchingUsers(false);
    }
  };

  const fetchInstitutions = async () => {
    setIsFetchingInstitutions(true);
    try {
      const res = await api.get('/institutions');
      setInstitutions(res.data);
    } catch (err) {
      console.error("Failed to fetch institutions", err);
    } finally {
      setIsFetchingInstitutions(false);
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleAddInstitution = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await api.post('/institutions', formData);
      setFormData({ name: '', address: '', domain: '', contactEmail: '', contactPhone: '' });
      setShowAddForm(false);
      fetchInstitutions(); // Refresh list
    } catch (err) {
      alert("Failed to add institution: " + (err.response?.data?.message || err.message));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerifyInstitution = async (id) => {
    try {
      await api.post(`/institutions/${id}/verify`);
      alert("Institution verified successfully!");
      fetchInstitutions();
      fetchAllUsers();
    } catch (err) {
      alert("Failed to verify institution: " + (err.response?.data?.message || err.message));
    }
  };

  const handleSuspendInstitution = async (id) => {
    if (!window.confirm("Are you sure you want to suspend this institution? This will deactivate its admin users.")) return;
    try {
      await api.post(`/institutions/${id}/suspend`);
      alert("Institution suspended successfully!");
      fetchInstitutions();
      fetchAllUsers();
    } catch (err) {
      alert("Failed to suspend institution: " + (err.response?.data?.message || err.message));
    }
  };

  const handleAssignRole = async (userId, newRole, institutionId) => {
    try {
      await api.post('/users/assign-role', { userId, newRole, institutionId });
      fetchAllUsers();
    } catch (err) {
      alert("Failed to assign role: " + (err.response?.data?.message || err.message));
    } finally {
      setIsAssigningRole(false);
    }
  };

  const handleRemoveRole = async (userId, role) => {
    if (!window.confirm(`Remove ${role} role from this user?`)) return;
    try {
      await api.delete(`/users/${userId}/roles/${role}`);
      await fetchAllUsers(); // Refresh
      alert('Role removed successfully');
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to remove role');
    }
  };

  const handleToggleUserStatus = async (userId) => {
    try {
      const res = await api.post(`/users/${userId}/toggle-status`);
      await fetchAllUsers(); // Refresh
      alert(res.data?.message || 'Status updated successfully');
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update user status');
    }
  };

  const handleUpdateUser = async (e, userId) => {
    e.preventDefault();
    try {
      await api.put(`/users/${userId}`, userEditData);
      alert('User details updated successfully');
      setEditingUser(null);
      fetchAllUsers();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update user');
    }
  };

  const handleAdminCreateUser = async (e) => {
    e.preventDefault();
    setIsSubmittingUser(true);
    try {
      await api.post('/users/admin-create', userFormData);
      alert('User created successfully!');
      setShowAddUserForm(false);
      setUserFormData({
        firstName: '', lastName: '', email: '', password: '', roleType: '', departmentId: ''
      });
      fetchAllUsers();
    } catch (error) {
      console.error("Failed to create user:", error);
      alert(error.response?.data?.message || "Failed to create user");
    } finally {
      setIsSubmittingUser(false);
    }
  };

  const handleCreateDepartment = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);
    try {
      await api.post('/departments', data);
      alert("Department created successfully!");
      if (selectedInstitution === data.institutionId || (!isSystemAdmin && user?.institutionId)) {
        fetchDepartments(selectedInstitution || user?.institutionId);
      }
      e.target.reset();
    } catch (err) {
      alert("Failed to create department: " + (err.response?.data?.message || err.message));
    }
  };

  const handleUpdateDepartment = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);
    try {
      await api.put(`/departments/${editingDepartment.id}`, data);
      alert("Department updated successfully!");
      fetchDepartments(editingDepartment.institutionId);
      setEditingDepartment(null);
    } catch (err) {
      alert("Failed to update department: " + (err.response?.data?.message || err.message));
    }
  };

  const handleAssignUserToDept = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);
    try {
      await api.post('/departments/assign-user', data);
      alert("User assigned to department successfully!");
      e.target.reset();
    } catch (err) {
      alert("Failed to assign user: " + (err.response?.data?.message || err.message));
    }
  };

  const handleCreateCategory = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);
    try {
      await api.post('/categories', data);
      alert("Category created successfully!");
      fetchCategories();
      e.target.reset();
    } catch (err) {
      alert("Failed to create category: " + (err.response?.data?.message || err.message));
    }
  };

  const handleAddEquipment = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);
    try {
      await api.post('/equipment', data);
      alert("Equipment added successfully!");
      if (selectedDepartment) {
        fetchEquipment('department', selectedDepartment);
      } else if (selectedInstitution) {
        fetchEquipment('institution', selectedInstitution);
      } else {
        fetchEquipment('global');
      }
      e.target.reset();
    } catch (err) {
      alert("Failed to add equipment: " + (err.response?.data?.message || err.message));
    }
  };

  const handleUpdateEquipmentStatus = async (equipmentId, newStatus) => {
    try {
      await api.patch(`/equipment/${equipmentId}/status?status=${newStatus}`);
      if (selectedDepartment) {
        fetchEquipment('department', selectedDepartment);
      } else if (selectedInstitution) {
        fetchEquipment('institution', selectedInstitution);
      } else {
        fetchEquipment('global');
      }
    } catch (err) {
      alert("Failed to update status: " + (err.response?.data?.message || err.message));
    }
  };

  const handleCreateBooking = async (e) => {
    e.preventDefault();
    try {
      await api.post('/bookings', {
        equipmentId: bookingData.equipmentId,
        startTime: bookingData.startTime,
        endTime: bookingData.endTime,
        purpose: bookingData.purpose
      });
      alert("Booking request submitted successfully!");
      setShowBookingModal(false);
      if (activeSection === 'bookings' || activeSection === 'book_equipment') fetchBookings();
    } catch (err) {
      alert("Failed to submit booking: " + (err.response?.data?.message || err.message));
    }
  };

  const handleUpdateBookingStatus = async (bookingId, newStatus) => {
    try {
      await api.patch(`/bookings/${bookingId}/status?status=${newStatus}`);
      fetchBookings();
    } catch (err) {
      alert("Failed to update booking status: " + (err.response?.data?.message || err.message));
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setPasswordMessage({ type: 'error', text: 'New passwords do not match.' });
      return;
    }
    setIsChangingPassword(true);
    setPasswordMessage({ type: '', text: '' });
    try {
      const res = await api.post('/users/change-password', {
        oldPassword: passwordData.oldPassword,
        newPassword: passwordData.newPassword
      });
      setPasswordMessage({ type: 'success', text: res.data.message || 'Password updated successfully!' });
      setPasswordData({ oldPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setPasswordMessage({ type: 'error', text: err.response?.data?.message || err.message });
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900 font-sans overflow-hidden">
      {/* Sidebar */}
      <aside className={`${isSidebarOpen ? 'w-64' : 'w-0'} transition-all duration-300 bg-[#eeeee4] text-gray-700 flex flex-col overflow-hidden whitespace-nowrap z-20 shrink-0 border-r border-gray-200`}>
        <div className="px-5 h-20 flex items-center border-b border-gray-300/60">
          {isSidebarOpen && (
            <div className="flex items-center gap-3">
              <span className="text-xl font-bold text-gray-900 tracking-tight">Lab Resource</span>
            </div>
          )}
        </div>
        <div className="flex-1 overflow-y-auto py-6 flex flex-col gap-1 px-4">
          <button onClick={() => setActiveSection('bookings')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'bookings' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
            <LayoutDashboard size={20} />
            <span>Dashboard</span>
          </button>
          <button onClick={() => setActiveSection('profile')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'profile' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
            <User size={20} />
            <span>Profiles</span>
          </button>

          {(isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && (
            <div className="mt-8 mb-3 px-4 text-[11px] font-bold text-gray-500 uppercase tracking-wider">
              Management
            </div>
          )}

          {isSystemAdmin && (
            <>
              <button onClick={() => setActiveSection('institutions')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'institutions' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
                <Building size={20} />
                <span>Institutions</span>
              </button>
            </>
          )}

          {(isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && (
            <>
              <button onClick={() => setActiveSection('departments')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'departments' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
                <Network size={20} />
                <span>Departments</span>
              </button>
              <button onClick={() => setActiveSection('categories')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'categories' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
                <Tags size={20} />
                <span>Categories</span>
              </button>
              <button onClick={() => {
                setActiveSection('equipment');
                if (!isSystemAdmin && user?.institutionId) {
                  setSelectedInstitution(user.institutionId);
                  fetchEquipment('institution', user.institutionId);
                }
              }} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'equipment' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
                <Settings size={20} />
                <span>Equipment</span>
              </button>
              <button onClick={() => {
                setActiveSection('book_equipment');
                setSelectedInstitution('');
                fetchEquipment('global');
              }} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'book_equipment' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
                <ShoppingCart size={20} />
                <span>Booking</span>
              </button>
            </>
          )}

          {(isSystemAdmin || hasRole('INSTITUTION_ADMIN')) && (
            <button onClick={() => setActiveSection('users')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'users' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
              <User size={20} />
              <span>Users</span>
            </button>
          )}
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col h-screen overflow-hidden bg-gray-50/50">
        {/* Top Navigation */}
        <header className="bg-white border-b border-gray-200/60 px-6 py-4 h-20 flex items-center justify-between shrink-0 z-10">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setIsSidebarOpen(!isSidebarOpen)}
              className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 transition-colors"
            >
              <MoreVertical size={22} />
            </button>
            <div className="flex items-center gap-3 lg:hidden">
              <img src="/logo.png" alt="Logo" className="w-7 h-7 object-contain" />
              <span className="text-lg font-bold text-gray-800 tracking-tight">LRUP</span>
            </div>
          </div>
          <div className="flex items-center gap-5">
            <div className="text-sm font-semibold text-gray-700 hidden sm:block bg-gray-100/70 px-4 py-2 rounded-full">
              {user?.firstName} {user?.lastName}
            </div>
            <button
              onClick={logout}
              className="flex items-center gap-2 text-red-500 hover:text-red-600 hover:bg-red-50 px-4 py-2.5 rounded-full font-semibold transition-colors text-sm"
            >
              <LogOut size={16} /> Logout
            </button>
          </div>
        </header>

        {/* Scrollable Content */}
        <main className="flex-1 overflow-y-auto p-6 lg:p-10">
          <div className="max-w-6xl mx-auto flex flex-col gap-8 pb-12">

            {/* Header Title based on Active Section */}
            {activeSection === 'profile' && (
              <div>
                <h1 className="text-3xl font-bold tracking-tight mb-2 text-gray-800">Profiles</h1>
                <p className="text-gray-500">Manage your personal profile and preferences.</p>
              </div>
            )}

            {activeSection !== 'profile' && (
              <div>
                <h1 className="text-3xl font-bold tracking-tight mb-2 text-gray-800">Dashboard</h1>
                <p className="text-gray-500">Welcome to your Lab Resource Utilization Platform.</p>
              </div>
            )}

            {/* User Profile Card */}
            {activeSection === 'profile' && (
              <section className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 animate-fade-in relative">
                <div className="flex items-center justify-between mb-8">
                  <h2 className="text-xl font-bold flex items-center gap-2 text-gray-800">
                    <User size={24} className="text-brand-orange" />
                    User Profile
                  </h2>

                  {!showPasswordForm && (
                    <button
                      onClick={() => setShowPasswordForm(true)}
                      className="bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 px-5 py-2 rounded-full font-semibold transition-colors text-sm flex items-center gap-2"
                    >
                      <Key size={16} />
                      Change Password
                    </button>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="bg-gray-50/80 p-6 rounded-2xl border border-gray-100">
                    <div className="text-gray-500 text-sm font-semibold mb-1 uppercase tracking-wide">Email Address</div>
                    <div className="text-lg font-bold text-gray-800">{user?.email || user?.username}</div>
                  </div>
                  {(user?.institutionName || user?.departmentName) && (
                    <div className="bg-gray-50/80 p-6 rounded-2xl border border-gray-100">
                      <div className="text-gray-500 text-sm font-semibold mb-1 uppercase tracking-wide">Organization</div>
                      <div className="text-lg font-bold text-gray-800">
                        {user?.institutionName}
                        {user?.departmentName && <span className="text-sm text-gray-500 block mt-1">Dept: {user?.departmentName}</span>}
                      </div>
                    </div>
                  )}
                </div>

                <div className="mt-6 bg-gray-50/80 p-6 rounded-2xl border border-gray-100">
                  <div className="text-gray-500 text-sm font-semibold mb-4 flex items-center gap-2 uppercase tracking-wide">
                    <Shield size={16} /> Assigned Roles & Authorities
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {(user?.roles || user?.authorities?.map(a => a.authority))?.map((auth, idx) => (
                      <span key={idx} className="bg-gradient-to-r from-brand-orange to-brand-pink text-white px-5 py-2 rounded-full text-xs font-bold shadow-sm tracking-wide">
                        {auth}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Change Password Section */}
                {showPasswordForm && (
                  <div className="mt-8 bg-gray-50/80 p-6 rounded-2xl border border-gray-100">
                    <div className="flex items-center justify-between mb-4">
                      <div className="text-gray-500 text-sm font-semibold flex items-center gap-2 uppercase tracking-wide">
                        <Key size={16} /> Change Password
                      </div>
                    </div>

                    {passwordMessage.text && (
                      <div className={`mb-4 px-4 py-2 rounded text-sm font-medium ${passwordMessage.type === 'error' ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                        {passwordMessage.text}
                      </div>
                    )}
                    <form onSubmit={handleChangePassword} className="flex flex-col gap-4 max-w-sm">
                      <div>
                        <input
                          type="password"
                          placeholder="Current Password"
                          required
                          className="rounded-xl border border-gray-300 px-4 py-2 outline-none w-full focus:border-brand-orange transition-colors"
                          value={passwordData.oldPassword}
                          onChange={(e) => setPasswordData({ ...passwordData, oldPassword: e.target.value })}
                        />
                      </div>
                      <div>
                        <input
                          type="password"
                          placeholder="New Password"
                          required
                          className="rounded-xl border border-gray-300 px-4 py-2 outline-none w-full focus:border-brand-orange transition-colors"
                          value={passwordData.newPassword}
                          onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
                        />
                      </div>
                      <div>
                        <input
                          type="password"
                          placeholder="Confirm New Password"
                          required
                          className="rounded-xl border border-gray-300 px-4 py-2 outline-none w-full focus:border-brand-orange transition-colors"
                          value={passwordData.confirmPassword}
                          onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
                        />
                      </div>
                      <div className="flex items-center gap-3 mt-2">
                        <button
                          type="button"
                          onClick={() => {
                            setShowPasswordForm(false);
                            setPasswordMessage({ type: '', text: '' });
                            setPasswordData({ oldPassword: '', newPassword: '', confirmPassword: '' });
                          }}
                          className="bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 px-5 py-2.5 rounded-full font-semibold transition-colors disabled:opacity-70 flex-1"
                          disabled={isChangingPassword}
                        >
                          Cancel
                        </button>
                        <button
                          type="submit"
                          disabled={isChangingPassword}
                          className="bg-brand-orange hover:bg-orange-600 text-white px-5 py-2.5 rounded-full font-semibold transition-colors disabled:opacity-70 flex-1 flex items-center justify-center gap-2"
                        >
                          {isChangingPassword ? <Loader2 size={16} className="animate-spin" /> : 'Update Password'}
                        </button>
                      </div>
                    </form>
                  </div>
                )}
              </section>
            )}

            {/* Bookings Section (Dashboard) */}
            {activeSection === 'bookings' && (
              <div className="flex flex-col gap-6 animate-fade-in">
                {/* Quick Stats Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex items-center gap-4">
                    <div className="bg-blue-50 p-4 rounded-xl text-blue-600"><Server size={24} /></div>
                    <div>
                      <div className="text-gray-500 text-sm font-medium">Total Equipment</div>
                      <div className="text-2xl font-bold text-gray-800">{dashboardStats.totalEquipment}</div>
                    </div>
                  </div>
                  <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex items-center gap-4">
                    <div className="bg-green-50 p-4 rounded-xl text-green-600"><Calendar size={24} /></div>
                    <div>
                      <div className="text-gray-500 text-sm font-medium">Today's Bookings</div>
                      <div className="text-2xl font-bold text-gray-800">{dashboardStats.todaysBookings}</div>
                    </div>
                  </div>
                  <div
                    className={`bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex items-center gap-4 ${isSystemAdmin ? 'cursor-pointer hover:bg-gray-50 transition-colors' : ''}`}
                    onClick={() => { if (isSystemAdmin) setActiveSection('institutions'); }}
                    title={isSystemAdmin ? "Click to view pending institutions" : ""}
                  >
                    <div className="bg-amber-50 p-4 rounded-xl text-amber-600"><Activity size={24} /></div>
                    <div>
                      <div className="text-gray-500 text-sm font-medium">Pending Approvals</div>
                      <div className="text-2xl font-bold text-gray-800">{dashboardStats.pendingApprovals}</div>
                    </div>
                  </div>
                  <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex items-center gap-4">
                    <div className="bg-red-50 p-4 rounded-xl text-red-600"><Wrench size={24} /></div>
                    <div>
                      <div className="text-gray-500 text-sm font-medium">Under Maintenance</div>
                      <div className="text-2xl font-bold text-gray-800">{dashboardStats.underMaintenance}</div>
                    </div>
                  </div>
                </div>

                {/* Header / Actions */}
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold text-gray-800 flex items-center gap-2">
                    <LayoutDashboard className="text-brand-orange" /> {isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER') ? 'All Bookings' : 'My Bookings'}
                  </h2>
                  <div className="flex items-center gap-3">
                    {(isSystemAdmin || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && (
                      <>
                        {(!isSystemAdmin && !hasRole('INSTITUTION_ADMIN') && (hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER'))) && (
                          <button onClick={() => setActiveSection('equipment')} className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-full text-sm font-medium transition-colors hidden md:flex items-center gap-2">
                            <Settings size={16} /> Add Equipment
                          </button>
                        )}
                        <button onClick={() => {
                          const el = document.getElementById('recent-bookings');
                          if (el) el.scrollIntoView({ behavior: 'smooth' });
                        }} className="bg-amber-100 hover:bg-amber-200 text-amber-700 px-4 py-2 rounded-full text-sm font-medium transition-colors hidden md:flex items-center gap-2">
                          <CheckCircle size={16} /> Approve Requests
                        </button>
                      </>
                    )}
                    {!isSystemAdmin && (
                      <button onClick={() => setActiveSection('book_equipment')} className="bg-brand-orange hover:bg-orange-600 text-white px-4 py-2 rounded-full text-sm font-medium transition-colors flex items-center gap-2 shadow-sm">
                        <Plus size={16} /> New Booking
                      </button>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6" id="recent-bookings">
                  {/* Recent Bookings Table */}
                  <div className="lg:col-span-2 bg-white rounded-2xl p-6 shadow-sm border border-gray-100">
                    <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2"><Calendar size={18} /> Recent Bookings</h3>
                    {isFetchingBookings ? (
                      <div className="flex justify-center p-8"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
                    ) : bookingsList.length > 0 ? (
                      <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse whitespace-nowrap">
                          <thead>
                            <tr className="border-b border-gray-200 text-xs uppercase tracking-wider text-gray-500">
                              <th className="pb-3 pr-4 font-medium">User</th>
                              <th className="pb-3 px-4 font-medium">Resource</th>
                              <th className="pb-3 px-4 font-medium">Time Slot</th>
                              <th className="pb-3 px-4 font-medium">Status</th>
                              <th className="pb-3 pl-4 font-medium">Action</th>
                            </tr>
                          </thead>
                          <tbody>
                            {bookingsList.map(b => (
                              <tr key={b.id} onClick={() => setSelectedBookingDetails(b)} className="cursor-pointer border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                                <td className="py-3 pr-4 font-medium text-gray-800 text-sm">{b.userName}</td>
                                <td className="py-3 px-4 text-gray-600 text-sm">{b.equipmentName}</td>
                                <td className="py-3 px-4 text-gray-500 text-xs">
                                  {new Date(b.startTime).toLocaleDateString()} {new Date(b.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </td>
                                <td className="py-3 px-4">
                                  <span className={`px-2 py-1 rounded text-[11px] font-bold tracking-wide ${b.status === 'CONFIRMED' ? 'bg-green-100 text-green-700' : b.status === 'PENDING' ? 'bg-amber-100 text-amber-700' : b.status === 'CANCELLED' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'}`}>
                                    {b.status}
                                  </span>
                                </td>
                                <td className="py-3 pl-4" onClick={(e) => e.stopPropagation()}>
                                  {(!isSystemAdmin && (hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && b.equipmentInstitutionId === user?.institutionId) ? (
                                    <select
                                      className="border border-gray-200 rounded px-2 py-1 text-xs bg-white text-gray-700 focus:outline-none focus:border-brand-orange"
                                      value={b.status}
                                      onChange={(e) => handleUpdateBookingStatus(b.id, e.target.value)}
                                    >
                                      <option value="PENDING">Pending</option>
                                      <option value="CONFIRMED">Confirmed</option>
                                      <option value="IN_USE">In Use</option>
                                      <option value="COMPLETED">Completed</option>
                                      <option value="CANCELLED">Cancelled</option>
                                      <option value="NO_SHOW">No Show</option>
                                    </select>
                                  ) : b.userId === user?.id && !['CANCELLED', 'COMPLETED'].includes(b.status) ? (
                                    <button
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        if (window.confirm('Are you sure you want to cancel this booking?')) {
                                          handleUpdateBookingStatus(b.id, 'CANCELLED');
                                        }
                                      }}
                                      className="text-red-600 hover:text-red-800 text-xs font-medium bg-red-50 hover:bg-red-100 px-2 py-1 rounded transition-colors border border-red-200"
                                    >
                                      Cancel Booking
                                    </button>
                                  ) : (
                                    <span className="text-gray-400 text-xs font-medium">View Only</span>
                                  )}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <div className="text-center p-8 text-gray-500 text-sm">No recent bookings found.</div>
                    )}
                  </div>

                  {/* Activity Feed */}
                  <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex flex-col h-full">
                    <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2"><Clock size={18} /> Recent Activity</h3>
                    <div className="flex-1 overflow-y-auto pr-2 space-y-4">
                      {bookingsList.slice(0, 8).map((b, i) => (
                        <div key={`act-${i}`} className="flex gap-3 items-start">
                          <div className="mt-0.5 bg-gray-100 p-1.5 rounded-full text-gray-500 shrink-0">
                            {b.status === 'CONFIRMED' ? <CheckCircle size={14} className="text-green-500" /> : <FileText size={14} />}
                          </div>
                          <div>
                            <p className="text-sm text-gray-800 font-medium leading-tight mb-1">{b.userName} <span className="font-normal text-gray-500">requested</span> {b.equipmentName}</p>
                            <span className="text-xs text-gray-400">{new Date(b.startTime).toLocaleDateString()} • {b.status.replace('_', ' ')}</span>
                          </div>
                        </div>
                      ))}
                      {bookingsList.length === 0 && <div className="text-sm text-gray-500">No activity yet.</div>}
                    </div>
                  </div>
                </div>

                {/* Analytics Section */}
                <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100">
                  <h3 className="font-semibold text-gray-800 mb-6 flex items-center gap-2"><Activity size={18} /> Most Used Equipment</h3>
                  {equipmentUsageData.length > 0 ? (
                    <div className="h-64 w-full">
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={equipmentUsageData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                          <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#6B7280' }} axisLine={false} tickLine={false} />
                          <YAxis tick={{ fontSize: 12, fill: '#6B7280' }} axisLine={false} tickLine={false} allowDecimals={false} />
                          <Tooltip cursor={{ fill: '#F3F4F6' }} contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                          <Bar dataKey="bookings" fill="#FF7A00" radius={[4, 4, 0, 0]} barSize={40} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  ) : (
                    <div className="text-center p-8 text-gray-500 text-sm">Not enough data to display trends.</div>
                  )}
                </div>
              </div>
            )}

            {/* 1. Institutions Section */}
            {isSystemAdmin && activeSection === 'institutions' && (
              <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in">
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-xl font-medium flex items-center gap-2">
                    <Building size={24} className="text-brand-pink" />
                    Institution Management
                  </h2>
                </div>

                {isFetchingInstitutions ? (
                  <div className="flex justify-center p-8"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
                ) : institutions.length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                      <thead>
                        <tr className="border-b border-gray-200"><th className="py-3 px-4 text-sm font-medium text-gray-500">Institute Name</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Domain</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Contact</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Status</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Action</th></tr>
                      </thead>
                      <tbody>
                        {institutions.map(inst => (
                          <tr key={inst.id} className="border-b border-gray-100 hover:bg-gray-50">
                            <td className="py-3 px-4 font-medium text-gray-800">{inst.name}</td>
                            <td className="py-3 px-4 text-gray-600">{inst.domain}</td>
                            <td className="py-3 px-4 text-gray-600">{inst.contactEmail}<br /><span className="text-xs text-gray-400">{inst.contactPhone}</span></td>
                            <td className="py-3 px-4">
                              {(inst.isActive || inst.active) ? (
                                <span className="px-2 py-1 rounded text-xs font-medium bg-green-100 text-green-700">Verified</span>
                              ) : (
                                <span className="px-2 py-1 rounded text-xs font-medium bg-yellow-100 text-yellow-700">Pending</span>
                              )}
                            </td>
                            <td className="py-3 px-4">
                              {!(inst.isActive || inst.active) ? (
                                <button onClick={() => handleVerifyInstitution(inst.id)} className="bg-brand-orange hover:bg-brand-pink text-white px-3 py-1 rounded text-sm font-medium transition-colors shadow-sm">
                                  Verify & Approve
                                </button>
                              ) : (
                                <button onClick={() => handleSuspendInstitution(inst.id)} className="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-sm font-medium transition-colors shadow-sm">
                                  Suspend
                                </button>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="text-center p-8 bg-gray-50 rounded-xl border border-dashed border-gray-300 text-gray-500">No institutions found. Click "Add Institute" to create one.</div>
                )}
              </section>
            )}

            {/* 2. Users Section */}
            {(isSystemAdmin || hasRole('INSTITUTION_ADMIN')) && activeSection === 'users' && (
              <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in">
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-xl font-medium flex items-center gap-2"><User size={24} className="text-brand-orange" /> User Management</h2>
                  {!isSystemAdmin && (
                    <button onClick={() => setShowAddUserForm(!showAddUserForm)} className="bg-gray-900 hover:bg-gray-800 text-white px-4 py-2 rounded-full text-sm font-medium transition-colors flex items-center gap-2">
                      <Plus size={16} /> {showAddUserForm ? 'Cancel' : 'Add User'}
                    </button>
                  )}
                </div>

                {!isSystemAdmin && showAddUserForm && (
                  <form onSubmit={handleAdminCreateUser} className="bg-gray-50 p-6 rounded-xl border border-gray-200 mb-8">
                    <h3 className="font-medium text-lg mb-4">Create New User</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                      <input type="text" placeholder="First Name" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" value={userFormData.firstName} onChange={(e) => setUserFormData({ ...userFormData, firstName: e.target.value })} />
                      <input type="text" placeholder="Last Name" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" value={userFormData.lastName} onChange={(e) => setUserFormData({ ...userFormData, lastName: e.target.value })} />
                      <input type="email" placeholder="Email Address" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" value={userFormData.email} onChange={(e) => setUserFormData({ ...userFormData, email: e.target.value })} />
                      <input type="text" placeholder="Initial Password" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" value={userFormData.password} onChange={(e) => setUserFormData({ ...userFormData, password: e.target.value })} />

                      <select required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full bg-white" value={userFormData.roleType} onChange={(e) => setUserFormData({ ...userFormData, roleType: e.target.value })}>
                        <option value="" disabled>Select Role...</option>
                        <option value="LAB_MANAGER">Lab Manager</option>
                        <option value="LAB_TECHNICIAN">Lab Technician</option>
                        <option value="RESEARCHER">Researcher</option>
                        <option value="STUDENT">Student</option>
                      </select>

                      <select className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full bg-white" value={userFormData.departmentId} onChange={(e) => setUserFormData({ ...userFormData, departmentId: e.target.value })}>
                        <option value="">Select Department (Optional)...</option>
                        {departments.map(d => <option key={d.id} value={d.id}>{d.name} ({d.institutionName})</option>)}
                      </select>
                    </div>
                    <button type="submit" disabled={isSubmittingUser} className="bg-gradient-to-r from-brand-orange to-brand-pink text-white px-6 py-2.5 rounded-full font-medium flex items-center justify-center gap-2 disabled:opacity-70">
                      {isSubmittingUser ? <Loader2 size={18} className="animate-spin" /> : 'Create User'}
                    </button>
                  </form>
                )}

                {isFetchingUsers ? (
                  <div className="flex justify-center p-8"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
                ) : usersList.length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                      <thead>
                        <tr className="border-b border-gray-200"><th className="py-3 px-4 text-sm font-medium text-gray-500">Name</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Email</th>{isSystemAdmin && <th className="py-3 px-4 text-sm font-medium text-gray-500">Institution</th>}<th className="py-3 px-4 text-sm font-medium text-gray-500">Status</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Current Roles</th>{!isSystemAdmin && <th className="py-3 px-4 text-sm font-medium text-gray-500">Assign Role</th>}{isSystemAdmin && <th className="py-3 px-4 text-sm font-medium text-gray-500">Action</th>}</tr>
                      </thead>
                      <tbody>
                        {usersList.filter(u => isSystemAdmin || !u.roles.some(r => r === 'SYSTEM_ADMIN' || r === 'INSTITUTION_ADMIN')).map(u => (
                          <tr key={u.id} className="border-b border-gray-100 hover:bg-gray-50">
                            {editingUser === u.id ? (
                              <td colSpan={isSystemAdmin ? "6" : "5"} className="p-0">
                                <form onSubmit={(e) => handleUpdateUser(e, u.id)} className="flex gap-2 p-3 bg-blue-50">
                                  <input type="text" value={userEditData.firstName} onChange={e => setUserEditData({ ...userEditData, firstName: e.target.value })} required className="border border-gray-300 px-3 py-1.5 rounded text-sm w-1/4" placeholder="First Name" />
                                  <input type="text" value={userEditData.lastName} onChange={e => setUserEditData({ ...userEditData, lastName: e.target.value })} required className="border border-gray-300 px-3 py-1.5 rounded text-sm w-1/4" placeholder="Last Name" />
                                  <input type="email" value={userEditData.email} onChange={e => setUserEditData({ ...userEditData, email: e.target.value })} required className="border border-gray-300 px-3 py-1.5 rounded text-sm w-1/4" placeholder="Email" />
                                  <button type="submit" className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm font-medium">Save</button>
                                  <button type="button" onClick={() => setEditingUser(null)} className="bg-gray-300 text-gray-700 px-4 py-1.5 rounded text-sm font-medium">Cancel</button>
                                </form>
                              </td>
                            ) : (
                              <>
                                <td className="py-3 px-4 font-medium text-gray-800">{u.firstName} {u.lastName}</td>
                                <td className="py-3 px-4 text-gray-600">{u.email}</td>
                                {isSystemAdmin && <td className="py-3 px-4 text-gray-600">{u.institutionName || '-'}</td>}
                                <td className="py-3 px-4">
                                  {u.isActive ? (
                                    <span className="px-2 py-1 rounded text-xs font-medium bg-green-100 text-green-700">Active</span>
                                  ) : (
                                    <span className="px-2 py-1 rounded text-xs font-medium bg-yellow-100 text-yellow-700">Pending</span>
                                  )}
                                </td>
                                <td className="py-3 px-4 text-gray-600"><div className="flex flex-wrap gap-1">{u.roles.map(r => (<span key={r} className="bg-gray-200 text-gray-700 px-2 py-0.5 rounded text-xs flex items-center gap-1">{r}{!isSystemAdmin && <button onClick={() => handleRemoveRole(u.id, r)} className="hover:text-red-500 font-bold leading-none" title="Remove role">&times;</button>}</span>))}</div></td>
                                {!isSystemAdmin && (
                                  <td className="py-3 px-4">
                                    <div className="flex gap-2 items-center">
                                      <select className="border border-gray-300 rounded px-2 py-1 text-sm bg-white" id={`roleSelect-${u.id}`} defaultValue="" onChange={(e) => {
                                        const instSelect = document.getElementById(`instSelect-${u.id}`);
                                        if (instSelect) {
                                          instSelect.style.display = e.target.value === 'INSTITUTION_ADMIN' ? 'block' : 'none';
                                        }
                                      }}>
                                        <option value="" disabled>Select Role...</option><option value="DEPT_HEAD">Dept Head</option><option value="LAB_MANAGER">Lab Manager</option><option value="LAB_ASSISTANT">Lab Assistant</option><option value="RESEARCHER">Researcher</option><option value="STUDENT">Student</option>
                                      </select>
                                      <select className="border border-gray-300 rounded px-2 py-1 text-sm bg-white" id={`instSelect-${u.id}`} defaultValue="" style={{ display: 'none' }}>
                                        <option value="" disabled>Select Institution...</option>
                                        {institutions.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                                      </select>
                                      <button onClick={() => {
                                        const role = document.getElementById(`roleSelect-${u.id}`).value;
                                        const inst = document.getElementById(`instSelect-${u.id}`).value;
                                        if (role === 'INSTITUTION_ADMIN' && !inst) {
                                          alert('Please select an institution for the Institution Admin.');
                                          return;
                                        }
                                        if (role) handleAssignRole(u.id, role, inst);
                                      }} className="bg-brand-orange hover:bg-brand-pink text-white px-3 py-1 rounded text-sm font-medium transition-colors">Add</button>
                                    </div>
                                  </td>
                                )}
                                {isSystemAdmin && (
                                  <td className="py-3 px-4">
                                    <div className="flex gap-2 items-center">
                                      {!u.isActive && (
                                        <button onClick={() => handleToggleUserStatus(u.id)} className="bg-brand-orange hover:bg-brand-pink text-white px-3 py-1 rounded text-sm font-medium transition-colors shadow-sm">
                                          Verify & Approve
                                        </button>
                                      )}
                                      {(!u.roles.includes('SYSTEM_ADMIN') || u.email === user?.email) && (
                                        <button onClick={() => { setEditingUser(u.id); setUserEditData({ firstName: u.firstName, lastName: u.lastName, email: u.email }); }} className="text-blue-600 hover:text-blue-800 font-medium text-sm">
                                          Edit
                                        </button>
                                      )}
                                    </div>
                                  </td>
                                )}
                              </>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="text-center p-8 bg-gray-50 rounded-xl border border-dashed border-gray-300 text-gray-500">No users found.</div>
                )}
              </section>
            )}

            {/* 3. Departments Section */}
            {activeSection === 'departments' && (
              <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in flex flex-col gap-8">
                <h2 className="text-xl font-medium flex items-center gap-2"><Network size={24} className="text-blue-500" /> Department Management</h2>

                {/* Create Department Form */}
                {(!isSystemAdmin && hasRole('LAB_MANAGER')) && (
                  <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
                    <h3 className="font-medium text-lg mb-4">Create New Department</h3>
                    <form onSubmit={handleCreateDepartment} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {isSystemAdmin ? (
                        <select name="institutionId" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full bg-white">
                          <option value="">Select Institution...</option>
                          {institutions.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                        </select>
                      ) : (
                        <select disabled value={user?.institutionId || ""} className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full bg-gray-100 text-gray-500">
                          <option value="">Select Institution...</option>
                          {institutions.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                        </select>
                      )}
                      {!isSystemAdmin && <input type="hidden" name="institutionId" value={user?.institutionId || ""} />}
                      <input type="text" name="name" placeholder="Department Name" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                      <input type="text" name="code" placeholder="Department Code (e.g. CS-01)" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                      <input type="text" name="domain" placeholder="Domain (Optional)" className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                      <input type="text" name="address" placeholder="Address (Optional)" className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                      <textarea name="description" placeholder="Description (Optional)" className="rounded-2xl border border-gray-300 px-5 py-3 outline-none w-full md:col-span-2" rows="2"></textarea>
                      <button type="submit" className="md:col-span-2 bg-blue-600 hover:bg-blue-700 text-white px-8 py-2.5 rounded-full font-medium transition-colors w-fit justify-self-start">Create Department</button>
                    </form>
                  </div>
                )}



                {/* View & Update Departments */}
                <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
                  <div className="flex justify-between items-center mb-4">
                    <h3 className="font-medium text-lg">View & Update Departments</h3>
                    {isSystemAdmin && (
                      <select value={selectedInstitution} onChange={(e) => { setSelectedInstitution(e.target.value); fetchDepartments(e.target.value); }} className="rounded-full border border-gray-300 px-4 py-1.5 outline-none text-sm bg-white">
                        <option value="">Filter by Institution...</option>
                        {institutions.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                      </select>
                    )}
                  </div>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                      <thead><tr className="border-b border-gray-200"><th className="py-3 px-4 text-sm font-medium text-gray-500">Code</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Name</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Description</th>{(isSystemAdmin || hasRole('LAB_MANAGER')) && <th className="py-3 px-4 text-sm font-medium text-gray-500 text-right">Actions</th>}</tr></thead>
                      <tbody>
                        {departments.map(d => (
                          <tr key={d.id} className="border-b border-gray-100 hover:bg-gray-50">
                            {editingDepartment?.id === d.id ? (
                              <td colSpan="4" className="p-0">
                                <form onSubmit={handleUpdateDepartment} className="flex gap-2 p-3 bg-blue-50">
                                  <input type="text" name="code" defaultValue={d.code} required className="border border-gray-300 px-3 py-1.5 rounded text-sm w-1/4" placeholder="Code" />
                                  <input type="text" name="name" defaultValue={d.name} required className="border border-gray-300 px-3 py-1.5 rounded text-sm w-1/4" placeholder="Name" />
                                  <input type="text" name="description" defaultValue={d.description} className="border border-gray-300 px-3 py-1.5 rounded text-sm w-1/2" placeholder="Description" />
                                  <button type="submit" className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm font-medium">Save</button>
                                  <button type="button" onClick={() => setEditingDepartment(null)} className="bg-gray-300 text-gray-700 px-4 py-1.5 rounded text-sm font-medium">Cancel</button>
                                </form>
                              </td>
                            ) : (
                              <>
                                <td className="py-3 px-4 text-gray-800 font-medium">{d.code}</td>
                                <td className="py-3 px-4 text-gray-800">{d.name}</td>
                                <td className="py-3 px-4 text-gray-600">{d.description || <span className="text-gray-400 italic">No description</span>}</td>
                                {(isSystemAdmin || hasRole('LAB_MANAGER')) && (
                                  <td className="py-3 px-4 text-right">
                                    <button onClick={() => setEditingDepartment(d)} className="text-blue-600 hover:text-blue-800 font-medium text-sm">Edit</button>
                                  </td>
                                )}
                              </>
                            )}
                          </tr>
                        ))}
                        {departments.length === 0 && (
                          <tr><td colSpan="4" className="py-6 text-center text-gray-500 italic">No departments found for this institution.</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </section>
            )}

            {/* 4. Categories Section */}
            {(isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && activeSection === 'categories' && (
              <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in flex flex-col gap-8">
                <h2 className="text-xl font-medium flex items-center gap-2"><Tags size={24} className="text-green-500" /> Equipment Categories</h2>
                {(!isSystemAdmin && (hasRole('LAB_MANAGER') || hasRole('DEPT_HEAD'))) && (
                  <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
                    <h3 className="font-medium text-lg mb-4">Create New Category</h3>
                    <form onSubmit={handleCreateCategory} className="flex flex-col gap-4">
                      <input type="text" name="name" placeholder="Category Name (e.g. Microscopes)" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                      <textarea name="description" placeholder="Description" className="rounded-2xl border border-gray-300 px-5 py-3 outline-none w-full" rows="2"></textarea>
                      <button type="submit" className="bg-green-600 hover:bg-green-700 text-white px-6 py-2.5 rounded-full font-medium transition-colors w-full sm:w-auto self-start">Create Category</button>
                    </form>
                  </div>
                )}

                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead><tr className="border-b border-gray-200"><th className="py-3 px-4 text-sm font-medium text-gray-500">Name</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Description</th></tr></thead>
                    <tbody>
                      {categories.map(c => (
                        <tr key={c.id} className="border-b border-gray-100 hover:bg-gray-50"><td className="py-3 px-4 font-medium text-gray-800">{c.name}</td><td className="py-3 px-4 text-gray-600">{c.description}</td></tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}

            {/* 5. Equipment Section */}
            {((activeSection === 'equipment') || (activeSection === 'book_equipment' && !isSystemAdmin)) && (
              <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in flex flex-col gap-8">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <h2 className="text-xl font-medium flex items-center gap-2">
                    <Settings size={24} className={activeSection === 'book_equipment' ? "text-brand-orange" : "text-purple-500"} />
                    {activeSection === 'book_equipment' ? 'Booking Management' : 'Equipment Management'}
                  </h2>
                  {activeSection === 'book_equipment' && !hasRole('INSTITUTION_ADMIN') && (
                    <div className="flex bg-gray-100 rounded-lg p-1">
                      <button onClick={() => setBookingTab('book')} className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${bookingTab === 'book' ? 'bg-white shadow-sm text-brand-orange' : 'text-gray-500 hover:text-gray-700'}`}>
                        Available Equipment
                      </button>
                      <button onClick={() => setBookingTab('history')} className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${bookingTab === 'history' ? 'bg-white shadow-sm text-brand-orange' : 'text-gray-500 hover:text-gray-700'}`}>
                        Booking History
                      </button>
                    </div>
                  )}
                </div>

                {(!isSystemAdmin && (hasRole('LAB_MANAGER') || hasRole('DEPT_HEAD'))) && activeSection === 'equipment' && (
                  <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
                    <h3 className="font-medium text-lg mb-4">Add New Equipment</h3>
                    <form onSubmit={handleAddEquipment} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <select name="categoryId" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full bg-white">
                        <option value="">Select Category...</option>
                        {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                      </select>
                      <div className="flex gap-2 w-full">
                        {isSystemAdmin ? (
                          <select value={selectedInstitution} onChange={(e) => { setSelectedInstitution(e.target.value); fetchDepartments(e.target.value); }} className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-1/2 bg-white">
                            <option value="">1. Filter Inst...</option>
                            {institutions.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                          </select>
                        ) : (
                          <div className="rounded-full border border-gray-300 px-5 py-2.5 bg-gray-100 text-gray-500 w-1/2 flex items-center overflow-hidden">
                            <span className="truncate">{user?.institutionName || 'Your Institution'}</span>
                          </div>
                        )}
                        <select name="departmentId" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-1/2 bg-white" disabled={isSystemAdmin && !selectedInstitution}>
                          <option value="">2. Select Dept...</option>
                          {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                        </select>
                      </div>
                      <input type="text" name="name" placeholder="Equipment Name" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                      <input type="text" name="serialNumber" placeholder="Serial Number" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                      <input type="text" name="manufacturer" placeholder="Manufacturer" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                      <input type="text" name="modelNumber" placeholder="Model Number" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                      <textarea name="description" placeholder="Description" className="rounded-2xl border border-gray-300 px-5 py-3 outline-none w-full md:col-span-2" rows="2"></textarea>
                      <button type="submit" className="md:col-span-2 bg-purple-600 hover:bg-purple-700 text-white px-8 py-2.5 rounded-full font-medium transition-colors w-fit justify-self-start">Add Equipment</button>
                    </form>
                  </div>

                )}

                {(activeSection !== 'book_equipment' || (bookingTab === 'book' && !hasRole('INSTITUTION_ADMIN'))) && (
                  <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
                    <h3 className="font-medium text-lg mb-4">{activeSection === 'book_equipment' ? 'Available Equipment for Booking' : 'View & Update Equipment'}</h3>
                    <div className="flex gap-2 w-full max-w-md mb-6">
                      {isSystemAdmin || activeSection === 'book_equipment' ? (
                        <select value={selectedInstitution} onChange={(e) => {
                          setSelectedInstitution(e.target.value);
                          setSelectedDepartment('');
                          fetchDepartments(e.target.value);
                          if (e.target.value) fetchEquipment('institution', e.target.value);
                          else fetchEquipment('global');
                        }} className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-1/2 bg-white">
                          <option value="">1. Filter Inst (All)</option>
                          {institutions.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                        </select>
                      ) : (
                        <div className="rounded-full border border-gray-300 px-5 py-2.5 bg-gray-100 text-gray-500 w-1/2 flex items-center overflow-hidden">
                          <span className="truncate">{user?.institutionName || 'Your Institution'}</span>
                        </div>
                      )}
                      <select value={selectedDepartment} onChange={(e) => {
                        setSelectedDepartment(e.target.value);
                        if (e.target.value) fetchEquipment('department', e.target.value);
                        else fetchEquipment('institution', selectedInstitution);
                      }} className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-1/2 bg-white" disabled={!selectedInstitution}>
                        <option value="">2. Select Dept (All)</option>
                        {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                      </select>
                    </div>

                    {(() => {
                      const renderTable = (items, isOtherInstitutes = false) => (
                        items.length > 0 ? (
                          <div className="overflow-x-auto border border-gray-200 rounded-xl">
                            <table className="w-full text-left border-collapse bg-white">
                              <thead><tr className="border-b border-gray-200 bg-gray-50"><th className="py-3 px-4 text-sm font-medium text-gray-500">Name & Details</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Institution</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Category</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Status</th>{!isSystemAdmin && !hasRole('INSTITUTION_ADMIN') && !isOtherInstitutes && activeSection !== 'book_equipment' && <th className="py-3 px-4 text-sm font-medium text-gray-500">Update Status</th>}{!isSystemAdmin && <th className="py-3 px-4 text-sm font-medium text-gray-500">Actions</th>}</tr></thead>
                              <tbody>
                                {items.map(eq => (
                                  <tr key={eq.id} className="border-b border-gray-100 hover:bg-gray-50">
                                    <td className="py-3 px-4"><div className="font-medium text-gray-800">{eq.name}</div><div className="text-xs text-gray-500">SN: {eq.serialNumber} | Mfr: {eq.manufacturer}</div></td>
                                    <td className="py-3 px-4 text-gray-600 text-sm">{eq.institutionName}<br /><span className="text-xs text-gray-400">{eq.departmentName}</span></td>
                                    <td className="py-3 px-4 text-gray-600 text-sm">{eq.categoryName}</td>
                                    <td className="py-3 px-4"><span className={`px-2 py-1 rounded text-xs font-medium ${eq.status === 'AVAILABLE' ? 'bg-green-100 text-green-700' : eq.status === 'BOOKED' ? 'bg-blue-100 text-blue-700' : 'bg-red-100 text-red-700'}`}>{eq.status}</span></td>
                                    {!isSystemAdmin && !hasRole('INSTITUTION_ADMIN') && !isOtherInstitutes && activeSection !== 'book_equipment' && (
                                      <td className="py-3 px-4">
                                        <select
                                          className="border border-gray-300 rounded px-2 py-1 text-sm bg-white disabled:bg-gray-100 disabled:text-gray-400"
                                          value={eq.status}
                                          onChange={(e) => handleUpdateEquipmentStatus(eq.id, e.target.value)}
                                          disabled={!isSystemAdmin && eq.institutionId !== user?.institutionId}
                                        >
                                          <option value="AVAILABLE">Available</option>
                                          <option value="BOOKED">Booked</option>
                                          <option value="UNDER_MAINTENANCE">Maintenance</option>
                                          <option value="OUT_OF_SERVICE">Out of Service</option>
                                          <option value="RETIRED">Retired</option>
                                          <option value="BROKEN">Broken</option>
                                        </select>
                                      </td>
                                    )}
                                    {!isSystemAdmin && (
                                      <td className="py-3 px-4">
                                        {(() => {
                                          if (eq.status !== 'AVAILABLE') return null;
                                          const isOwnInstitution = eq.institutionId === user?.institutionId;
                                          if (hasRole('INSTITUTION_ADMIN')) return null;
                                          const isManagerRole = hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER');
                                          // Hide button if they are a manager/admin viewing their OWN institution's equipment
                                          if (isManagerRole && isOwnInstitution) return null;

                                          return (
                                            <button
                                              onClick={() => { setBookingData({ ...bookingData, equipmentId: eq.id, equipmentName: eq.name }); setShowBookingModal(true); }}
                                              className="bg-brand-orange hover:bg-orange-600 text-white px-3 py-1.5 rounded-full text-xs font-medium transition-colors"
                                            >
                                              Book
                                            </button>
                                          );
                                        })()}
                                      </td>
                                    )}
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        ) : (
                          <div className="text-center p-8 bg-white rounded-xl border border-dashed border-gray-300 text-gray-500">No equipment found.</div>
                        )
                      );

                      if (!isSystemAdmin && !selectedInstitution) {
                        const myEquipment = equipmentList.filter(eq => eq.institutionId === user?.institutionId);
                        const otherEquipment = equipmentList.filter(eq => eq.institutionId !== user?.institutionId);

                        const paginatedOther = activeSection === 'book_equipment'
                          ? otherEquipment.slice((equipmentPage - 1) * itemsPerPage, equipmentPage * itemsPerPage)
                          : otherEquipment;
                        const totalOtherPages = Math.ceil(otherEquipment.length / itemsPerPage);

                        return (
                          <div className="flex flex-col gap-8">
                            {activeSection === 'equipment' && (
                              <div>
                                <h4 className="font-semibold text-lg text-gray-700 mb-3">My Institute Equipment</h4>
                                {renderTable(myEquipment, false)}
                              </div>
                            )}
                            <div>
                              <h4 className="font-semibold text-lg text-gray-700 mb-3">Other Institutes' Equipment</h4>
                              {renderTable(paginatedOther, true)}

                              {activeSection === 'book_equipment' && totalOtherPages > 1 && (
                                <div className="flex justify-center items-center mt-4 gap-2">
                                  <button onClick={() => setEquipmentPage(Math.max(1, equipmentPage - 1))} disabled={equipmentPage === 1} className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50">Prev</button>
                                  <span className="text-sm text-gray-600">Page {equipmentPage} of {totalOtherPages}</span>
                                  <button onClick={() => setEquipmentPage(Math.min(totalOtherPages, equipmentPage + 1))} disabled={equipmentPage === totalOtherPages} className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50">Next</button>
                                </div>
                              )}
                            </div>
                          </div>
                        );
                      }

                      return renderTable(equipmentList, false);
                    })()}
                  </div>
                )}
              </section>
            )}

            {/* 6. Booking History View */}
            {(activeSection === 'book_equipment' && (bookingTab === 'history' || hasRole('INSTITUTION_ADMIN') || isSystemAdmin)) && (
              <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in flex flex-col gap-8">
                <h2 className="text-xl font-medium flex items-center gap-2"><ShoppingCart size={24} className="text-brand-orange" /> {isSystemAdmin ? 'Global Bookings Overview' : 'Booking History'}</h2>
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse whitespace-nowrap">
                    <thead>
                      <tr className="border-b border-gray-200 text-xs uppercase tracking-wider text-gray-500">
                        <th className="pb-3 pr-4 font-medium">User</th>
                        <th className="pb-3 px-4 font-medium">User's Institute</th>
                        <th className="pb-3 px-4 font-medium">Equipment</th>
                        <th className="pb-3 px-4 font-medium">Equipment's Institute</th>
                        <th className="pb-3 px-4 font-medium">Time Slot</th>
                        <th className="pb-3 px-4 font-medium">Status</th>
                        <th className="pb-3 px-4 font-medium">Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(() => {
                        const historyBookings = isSystemAdmin ? bookingsList : bookingsList.filter(b => b.userId === user?.id || b.userInstitutionId === user?.institutionId);
                        const sortedHistoryBookings = [...historyBookings].sort((a, b) => new Date(b.startTime).getTime() < new Date(a.startTime).getTime() ? 1 : -1);
                        const paginatedBookings = sortedHistoryBookings.slice((bookingHistoryPage - 1) * itemsPerPage, bookingHistoryPage * itemsPerPage);
                        const totalPages = Math.ceil(sortedHistoryBookings.length / itemsPerPage);

                        if (sortedHistoryBookings.length === 0) {
                          return (
                            <tr>
                              <td colSpan="7" className="py-8 text-center text-gray-500">No bookings found in the system.</td>
                            </tr>
                          );
                        }

                        return paginatedBookings.map(b => (
                          <tr key={b.id} onClick={() => setSelectedBookingDetails(b)} className="cursor-pointer border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                            <td className="py-3 pr-4 font-medium text-gray-800 text-sm">{b.userName}</td>
                            <td className="py-3 px-4 text-gray-600 text-sm">{b.userInstitutionName || 'N/A'}</td>
                            <td className="py-3 px-4 text-gray-800 text-sm font-medium">{b.equipmentName}</td>
                            <td className="py-3 px-4 text-gray-600 text-sm">{b.equipmentInstitutionName || 'N/A'}</td>
                            <td className="py-3 px-4 text-gray-500 text-xs">
                              {new Date(b.startTime).toLocaleDateString()} {new Date(b.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </td>
                            <td className="py-3 px-4">
                              <span className={`px-2 py-1 rounded text-[11px] font-bold tracking-wide ${b.status === 'CONFIRMED' ? 'bg-green-100 text-green-700' : b.status === 'PENDING' ? 'bg-amber-100 text-amber-700' : b.status === 'CANCELLED' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'}`}>
                                {b.status}
                              </span>
                            </td>
                            <td className="py-3 pl-4" onClick={(e) => e.stopPropagation()}>
                              {(!isSystemAdmin && (hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && b.equipmentInstitutionId === user?.institutionId) ? (
                                <select
                                  className="border border-gray-200 rounded px-2 py-1 text-xs bg-white text-gray-700 focus:outline-none focus:border-brand-orange"
                                  value={b.status}
                                  onChange={(e) => handleUpdateBookingStatus(b.id, e.target.value)}
                                >
                                  <option value="PENDING">Pending</option>
                                  <option value="CONFIRMED">Confirmed</option>
                                  <option value="IN_USE">In Use</option>
                                  <option value="COMPLETED">Completed</option>
                                  <option value="CANCELLED">Cancelled</option>
                                  <option value="NO_SHOW">No Show</option>
                                </select>
                              ) : b.userId === user?.id && !['CANCELLED', 'COMPLETED'].includes(b.status) ? (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    if (window.confirm('Are you sure you want to cancel this booking?')) {
                                      handleUpdateBookingStatus(b.id, 'CANCELLED');
                                    }
                                  }}
                                  className="text-red-600 hover:text-red-800 text-xs font-medium bg-red-50 hover:bg-red-100 px-2 py-1 rounded transition-colors border border-red-200"
                                >
                                  Cancel Booking
                                </button>
                              ) : (
                                <span className="text-gray-400 text-xs font-medium">View Only</span>
                              )}
                            </td>
                          </tr>
                        ));
                      })()}
                    </tbody>
                  </table>
                </div>
                {(() => {
                  const historyBookings = isSystemAdmin ? bookingsList : bookingsList.filter(b => b.userId === user?.id || b.userInstitutionId === user?.institutionId);
                  const totalPages = Math.ceil(historyBookings.length / itemsPerPage);
                  if (totalPages > 1) {
                    return (
                      <div className="flex justify-center items-center mt-4 gap-2">
                        <button onClick={() => setBookingHistoryPage(Math.max(1, bookingHistoryPage - 1))} disabled={bookingHistoryPage === 1} className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50">Prev</button>
                        <span className="text-sm text-gray-600">Page {bookingHistoryPage} of {totalPages}</span>
                        <button onClick={() => setBookingHistoryPage(Math.min(totalPages, bookingHistoryPage + 1))} disabled={bookingHistoryPage === totalPages} className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50">Next</button>
                      </div>
                    );
                  }
                  return null;
                })()}
              </section>
            )}
          </div>
        </main>

        {/* Booking Modal */}
        {showBookingModal && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fade-in p-4">
            <div className="bg-white rounded-2xl p-8 max-w-md w-full shadow-xl">
              <h3 className="text-xl font-medium mb-2">Book Equipment</h3>
              <p className="text-gray-500 text-sm mb-6">You are booking: <span className="font-medium text-gray-800">{bookingData.equipmentName}</span></p>

              <form onSubmit={handleCreateBooking} className="flex flex-col gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Start Time</label>
                  <input
                    type="datetime-local"
                    required
                    className="rounded-xl border border-gray-300 px-4 py-2.5 outline-none w-full"
                    value={bookingData.startTime}
                    onChange={(e) => setBookingData({ ...bookingData, startTime: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">End Time</label>
                  <input
                    type="datetime-local"
                    required
                    className="rounded-xl border border-gray-300 px-4 py-2.5 outline-none w-full"
                    value={bookingData.endTime}
                    onChange={(e) => setBookingData({ ...bookingData, endTime: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Purpose</label>
                  <textarea
                    required
                    placeholder="Why do you need this equipment?"
                    className="rounded-xl border border-gray-300 px-4 py-2.5 outline-none w-full"
                    rows="3"
                    value={bookingData.purpose}
                    onChange={(e) => setBookingData({ ...bookingData, purpose: e.target.value })}
                  ></textarea>
                </div>

                <div className="flex justify-end gap-3 mt-4">
                  <button
                    type="button"
                    onClick={() => setShowBookingModal(false)}
                    className="px-5 py-2.5 rounded-full text-gray-600 hover:bg-gray-100 font-medium transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="bg-brand-orange hover:bg-orange-600 text-white px-5 py-2.5 rounded-full font-medium transition-colors"
                  >
                    Confirm Booking
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
      {/* Booking Details Modal */}
      {selectedBookingDetails && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white rounded-2xl p-8 max-w-lg w-full shadow-2xl relative">
            <button onClick={() => setSelectedBookingDetails(null)} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 bg-gray-50 hover:bg-gray-100 p-2 rounded-full transition-colors"><X size={20} /></button>
            <h3 className="text-2xl font-bold text-gray-800 mb-6 flex items-center gap-3">
              <div className="bg-blue-50 p-2 rounded-lg text-blue-600"><Calendar size={24} /></div>
              Booking Details
            </h3>
            
            <div className="space-y-4">
              <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
                <span className="text-gray-500 font-medium col-span-1">Status:</span>
                <span className="col-span-2 font-semibold">
                  <span className={`px-2 py-1 rounded text-xs tracking-wide ${selectedBookingDetails.status === 'CONFIRMED' ? 'bg-green-100 text-green-700' : selectedBookingDetails.status === 'PENDING' ? 'bg-amber-100 text-amber-700' : selectedBookingDetails.status === 'CANCELLED' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'}`}>
                    {selectedBookingDetails.status}
                  </span>
                </span>
              </div>
              
              <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
                <span className="text-gray-500 font-medium col-span-1">Equipment:</span>
                <span className="col-span-2 text-gray-800 font-medium">{selectedBookingDetails.equipmentName}</span>
              </div>

              <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
                <span className="text-gray-500 font-medium col-span-1">Owner Inst:</span>
                <span className="col-span-2 text-gray-800">{selectedBookingDetails.equipmentInstitutionName || 'N/A'}</span>
              </div>

              <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
                <span className="text-gray-500 font-medium col-span-1">Booked By:</span>
                <span className="col-span-2 text-gray-800">{selectedBookingDetails.userName} <span className="text-sm text-gray-500">({selectedBookingDetails.userInstitutionName || 'N/A'})</span></span>
              </div>

              <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
                <span className="text-gray-500 font-medium col-span-1">Start Time:</span>
                <span className="col-span-2 text-gray-800">
                  {new Date(selectedBookingDetails.startTime).toLocaleDateString()} {new Date(selectedBookingDetails.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                </span>
              </div>

              <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
                <span className="text-gray-500 font-medium col-span-1">End Time:</span>
                <span className="col-span-2 text-gray-800">
                  {new Date(selectedBookingDetails.endTime).toLocaleDateString()} {new Date(selectedBookingDetails.endTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                </span>
              </div>

              <div className="grid grid-cols-1 gap-2 pt-2">
                <span className="text-gray-500 font-medium">Purpose:</span>
                <p className="bg-gray-50 p-4 rounded-xl text-gray-700 text-sm whitespace-pre-wrap border border-gray-100">
                  {selectedBookingDetails.purpose || 'No purpose provided.'}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
