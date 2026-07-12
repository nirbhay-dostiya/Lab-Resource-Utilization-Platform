import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Shield, Key, Building, Plus, Loader2, Server, ExternalLink, Network, Tags, Settings, ChevronDown, ChevronUp } from 'lucide-react';
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
  
  // Form visibility state
  const [activeSection, setActiveSection] = useState('institutions'); // institutions, users, departments, categories, equipment
  
  // Add Institution Form State
  const [showAddForm, setShowAddForm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    address: '',
    domain: '',
    contactEmail: '',
    contactPhone: ''
  });
  const [editingDepartment, setEditingDepartment] = useState(null);

  const isSystemAdmin = user?.roles?.includes('SYSTEM_ADMIN') || user?.authorities?.some(auth => auth.authority === 'SYSTEM_ADMIN');

  useEffect(() => {
    if (isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD')) {
      fetchInstitutions();
      fetchAllUsers();
      fetchCategories();
    }
  }, [isSystemAdmin, user]);

  useEffect(() => {
    if (user && !isSystemAdmin && user.institutionId && !selectedInstitution) {
      setSelectedInstitution(user.institutionId);
      fetchDepartments(user.institutionId);
    }
  }, [user, isSystemAdmin, selectedInstitution]);

  useEffect(() => {
    if (activeSection === 'equipment') {
      if (selectedDepartment) {
        fetchEquipment('department', selectedDepartment);
      } else if (selectedInstitution) {
        fetchEquipment('institution', selectedInstitution);
      } else {
        fetchEquipment('global');
      }
    }
  }, [activeSection]);

  const hasRole = (roleName) => user?.roles?.includes(roleName) || user?.authorities?.includes(roleName) || user?.authorities?.some(auth => auth.authority === roleName);

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
    if (!window.confirm(`Are you sure you want to remove the ${role} role?`)) return;
    try {
      await api.delete(`/users/${userId}/roles/${role}`);
      fetchAllUsers(); // Refresh list to show updated roles
    } catch (err) {
      alert("Failed to remove role: " + (err.response?.data?.message || err.message));
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

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 font-sans">
      {/* Top Navigation */}
      <header className="bg-white border-b border-gray-200 px-8 py-4 flex items-center justify-between sticky top-0 z-10">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-brand-pink to-brand-orange"></div>
          <span className="text-xl font-semibold tracking-tight">LabManager</span>
        </div>
        <div className="flex items-center gap-6">
          <div className="text-sm font-medium text-gray-600 hidden sm:block">
            {user?.firstName} {user?.lastName}
          </div>
          <button 
            onClick={logout} 
            className="flex items-center gap-2 text-red-600 hover:text-red-700 bg-red-50 hover:bg-red-100 px-4 py-2 rounded-full font-medium transition-colors text-sm"
          >
            <LogOut size={16} /> Logout
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-5xl mx-auto px-6 py-10 flex flex-col gap-8">
        
        {/* Welcome Section */}
        <div>
          <h1 className="text-3xl font-medium tracking-tight mb-2">Dashboard</h1>
          <p className="text-gray-500">Welcome to your Lab Resource Utilization Platform.</p>
        </div>

        {/* User Profile Card */}
        <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200">
          <h2 className="text-xl font-medium flex items-center gap-2 mb-6">
            <User size={24} className="text-brand-orange" />
            User Profile
          </h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-gray-50 p-5 rounded-xl border border-gray-100">
              <div className="text-gray-500 text-sm mb-1">Email Address</div>
              <div className="text-lg font-medium text-gray-800">{user?.email || user?.username}</div>
            </div>
            {(user?.institutionName || user?.departmentName) && (
              <div className="bg-gray-50 p-5 rounded-xl border border-gray-100">
                <div className="text-gray-500 text-sm mb-1">Organization</div>
                <div className="text-lg font-medium text-gray-800">
                  {user?.institutionName}
                  {user?.departmentName && <span className="text-sm text-gray-500 block mt-1">Dept: {user?.departmentName}</span>}
                </div>
              </div>
            )}
          </div>

          <div className="mt-6 bg-gray-50 p-5 rounded-xl border border-gray-100">
              <div className="text-gray-500 text-sm mb-3 flex items-center gap-1.5">
                <Shield size={16} /> Assigned Roles & Authorities
              </div>
              <div className="flex flex-wrap gap-2">
                {(user?.roles || user?.authorities?.map(a => a.authority))?.map((auth, idx) => (
                  <span key={idx} className="bg-gradient-to-r from-brand-orange to-brand-pink text-white px-4 py-1.5 rounded-full text-xs font-medium shadow-sm">
                    {auth}
                  </span>
                ))}
              </div>
            </div>
        </section>

        {/* Admin Navigation Tabs */}
        {(isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD')) && (
          <div className="flex flex-wrap gap-2 mb-2 bg-white p-2 rounded-xl border border-gray-200 shadow-sm">
            {isSystemAdmin && (
              <>
                <button onClick={() => setActiveSection('institutions')} className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 ${activeSection === 'institutions' ? 'bg-brand-orange text-white' : 'text-gray-600 hover:bg-gray-100'}`}><Building size={16} /> Institutions</button>
                <button onClick={() => setActiveSection('users')} className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 ${activeSection === 'users' ? 'bg-brand-orange text-white' : 'text-gray-600 hover:bg-gray-100'}`}><User size={16} /> Users</button>
              </>
            )}
            {(isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD')) && (
              <button onClick={() => setActiveSection('categories')} className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 ${activeSection === 'categories' ? 'bg-brand-orange text-white' : 'text-gray-600 hover:bg-gray-100'}`}><Tags size={16} /> Categories</button>
            )}
            <button onClick={() => setActiveSection('departments')} className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 ${activeSection === 'departments' ? 'bg-brand-orange text-white' : 'text-gray-600 hover:bg-gray-100'}`}><Network size={16} /> Departments</button>
            <button onClick={() => setActiveSection('equipment')} className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 ${activeSection === 'equipment' ? 'bg-brand-orange text-white' : 'text-gray-600 hover:bg-gray-100'}`}><Settings size={16} /> Equipment</button>
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
              <button onClick={() => setShowAddForm(!showAddForm)} className="bg-gray-900 hover:bg-gray-800 text-white px-4 py-2 rounded-full text-sm font-medium transition-colors flex items-center gap-2">
                <Plus size={16} /> {showAddForm ? 'Cancel' : 'Add Institute'}
              </button>
            </div>
            
            {showAddForm && (
              <form onSubmit={handleAddInstitution} className="bg-gray-50 p-6 rounded-xl border border-gray-200 mb-8">
                <h3 className="font-medium text-lg mb-4">Create New Institution</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                  <input type="text" name="name" placeholder="Institution Name" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" value={formData.name} onChange={handleChange} />
                  <input type="text" name="domain" placeholder="Domain (e.g. mit.edu)" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" value={formData.domain} onChange={handleChange} />
                  <input type="email" name="contactEmail" placeholder="Contact Email" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" value={formData.contactEmail} onChange={handleChange} />
                  <input type="text" name="contactPhone" placeholder="Contact Phone" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" value={formData.contactPhone} onChange={handleChange} />
                  <input type="text" name="address" placeholder="Physical Address" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full md:col-span-2" value={formData.address} onChange={handleChange} />
                </div>
                <button type="submit" disabled={isSubmitting} className="bg-gradient-to-r from-brand-orange to-brand-pink text-white px-6 py-2.5 rounded-full font-medium flex items-center justify-center gap-2 disabled:opacity-70">
                  {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : 'Save Institution'}
                </button>
              </form>
            )}

            {isFetchingInstitutions ? (
              <div className="flex justify-center p-8"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
            ) : institutions.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-gray-200"><th className="py-3 px-4 text-sm font-medium text-gray-500">Name</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Domain</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Contact</th></tr>
                  </thead>
                  <tbody>
                    {institutions.map(inst => (
                      <tr key={inst.id} className="border-b border-gray-100 hover:bg-gray-50"><td className="py-3 px-4 font-medium text-gray-800">{inst.name}</td><td className="py-3 px-4 text-gray-600">{inst.domain}</td><td className="py-3 px-4 text-gray-600">{inst.contactEmail}<br/><span className="text-xs text-gray-400">{inst.contactPhone}</span></td></tr>
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
        {isSystemAdmin && activeSection === 'users' && (
          <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in">
            <h2 className="text-xl font-medium flex items-center gap-2 mb-6"><User size={24} className="text-brand-orange" /> User Management</h2>
            {isFetchingUsers ? (
              <div className="flex justify-center p-8"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
            ) : usersList.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-gray-200"><th className="py-3 px-4 text-sm font-medium text-gray-500">Name</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Email</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Current Roles</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Assign Role</th></tr>
                  </thead>
                  <tbody>
                    {usersList.map(u => (
                      <tr key={u.id} className="border-b border-gray-100 hover:bg-gray-50">
                        <td className="py-3 px-4 font-medium text-gray-800">{u.firstName} {u.lastName}</td>
                        <td className="py-3 px-4 text-gray-600">{u.email}</td>
                        <td className="py-3 px-4 text-gray-600"><div className="flex flex-wrap gap-1">{u.roles.map(r => (<span key={r} className="bg-gray-200 text-gray-700 px-2 py-0.5 rounded text-xs flex items-center gap-1">{r}<button onClick={() => handleRemoveRole(u.id, r)} className="hover:text-red-500 font-bold leading-none" title="Remove role">&times;</button></span>))}</div></td>
                        <td className="py-3 px-4">
                          <div className="flex gap-2 items-center">
                            <select className="border border-gray-300 rounded px-2 py-1 text-sm bg-white" id={`roleSelect-${u.id}`} defaultValue="" onChange={(e) => {
                              const instSelect = document.getElementById(`instSelect-${u.id}`);
                              if (instSelect) {
                                instSelect.style.display = e.target.value === 'INSTITUTION_ADMIN' ? 'block' : 'none';
                              }
                            }}>
                              <option value="" disabled>Select Role...</option><option value="SYSTEM_ADMIN">System Admin</option><option value="INSTITUTION_ADMIN">Inst. Admin</option><option value="DEPT_HEAD">Dept Head</option><option value="LAB_MANAGER">Lab Manager</option><option value="LAB_ASSISTANT">Lab Assistant</option><option value="RESEARCHER">Researcher</option><option value="STUDENT">Student</option>
                            </select>
                            <select className="border border-gray-300 rounded px-2 py-1 text-sm bg-white" id={`instSelect-${u.id}`} defaultValue="" style={{display: 'none'}}>
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
                <button type="submit" className="md:col-span-2 bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-full font-medium transition-colors">Create Department</button>
              </form>
            </div>

            {/* Assign User Form */}
            {isSystemAdmin && (
              <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
                <h3 className="font-medium text-lg mb-4">Assign User to Department</h3>
                <form onSubmit={handleAssignUserToDept} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <select name="userId" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full bg-white">
                    <option value="">Select User...</option>
                    {usersList.map(u => <option key={u.id} value={u.id}>{u.firstName} {u.lastName} ({u.email})</option>)}
                  </select>
                  <div className="flex gap-2 w-full">
                    <select value={selectedInstitution} onChange={(e) => {setSelectedInstitution(e.target.value); fetchDepartments(e.target.value);}} className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-1/2 bg-white">
                      <option value="">1. Filter Inst...</option>
                      {institutions.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                    </select>
                    <select name="departmentId" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-1/2 bg-white" disabled={!selectedInstitution}>
                      <option value="">2. Select Dept...</option>
                      {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                    </select>
                  </div>
                  <button type="submit" className="md:col-span-2 bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-full font-medium transition-colors">Assign User</button>
                </form>
              </div>
            )}

            {/* View & Update Departments */}
            <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
              <div className="flex justify-between items-center mb-4">
                <h3 className="font-medium text-lg">View & Update Departments</h3>
                {isSystemAdmin && (
                  <select value={selectedInstitution} onChange={(e) => {setSelectedInstitution(e.target.value); fetchDepartments(e.target.value);}} className="rounded-full border border-gray-300 px-4 py-1.5 outline-none text-sm bg-white">
                    <option value="">Filter by Institution...</option>
                    {institutions.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                  </select>
                )}
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead><tr className="border-b border-gray-200"><th className="py-3 px-4 text-sm font-medium text-gray-500">Code</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Name</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Description</th><th className="py-3 px-4 text-sm font-medium text-gray-500 text-right">Actions</th></tr></thead>
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
                            <td className="py-3 px-4 text-right">
                              <button onClick={() => setEditingDepartment(d)} className="text-blue-600 hover:text-blue-800 font-medium text-sm">Edit</button>
                            </td>
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
        {(isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD')) && activeSection === 'categories' && (
          <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in flex flex-col gap-8">
            <h2 className="text-xl font-medium flex items-center gap-2"><Tags size={24} className="text-green-500" /> Equipment Categories</h2>
            <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
              <h3 className="font-medium text-lg mb-4">Create New Category</h3>
              <form onSubmit={handleCreateCategory} className="flex flex-col gap-4">
                <input type="text" name="name" placeholder="Category Name (e.g. Microscopes)" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                <textarea name="description" placeholder="Description" className="rounded-2xl border border-gray-300 px-5 py-3 outline-none w-full" rows="2"></textarea>
                <button type="submit" className="bg-green-600 hover:bg-green-700 text-white px-6 py-2.5 rounded-full font-medium transition-colors w-full sm:w-auto self-start">Create Category</button>
              </form>
            </div>
            
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
        {activeSection === 'equipment' && (
          <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in flex flex-col gap-8">
            <h2 className="text-xl font-medium flex items-center gap-2"><Settings size={24} className="text-purple-500" /> Equipment Management</h2>
            
            <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
              <h3 className="font-medium text-lg mb-4">Add New Equipment</h3>
              <form onSubmit={handleAddEquipment} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <select name="categoryId" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full bg-white">
                  <option value="">Select Category...</option>
                  {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
                <div className="flex gap-2 w-full">
                  <select disabled={!isSystemAdmin} value={selectedInstitution} onChange={(e) => {setSelectedInstitution(e.target.value); fetchDepartments(e.target.value);}} className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-1/2 bg-white disabled:bg-gray-100 disabled:text-gray-500">
                    <option value="">1. Filter Inst...</option>
                    {institutions.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
                  </select>
                  <select name="departmentId" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-1/2 bg-white" disabled={!selectedInstitution}>
                    <option value="">2. Select Dept...</option>
                    {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                  </select>
                </div>
                <input type="text" name="name" placeholder="Equipment Name" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                <input type="text" name="serialNumber" placeholder="Serial Number" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                <input type="text" name="manufacturer" placeholder="Manufacturer" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                <input type="text" name="modelNumber" placeholder="Model Number" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                <textarea name="description" placeholder="Description" className="rounded-2xl border border-gray-300 px-5 py-3 outline-none w-full md:col-span-2" rows="2"></textarea>
                <button type="submit" className="md:col-span-2 bg-purple-600 hover:bg-purple-700 text-white px-6 py-2.5 rounded-full font-medium transition-colors">Add Equipment</button>
              </form>
            </div>

            <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
              <h3 className="font-medium text-lg mb-4">View & Update Equipment</h3>
              <div className="flex gap-2 w-full max-w-md mb-6">
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
                <select value={selectedDepartment} onChange={(e) => {
                  setSelectedDepartment(e.target.value); 
                  if (e.target.value) fetchEquipment('department', e.target.value);
                  else fetchEquipment('institution', selectedInstitution);
                }} className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-1/2 bg-white" disabled={!selectedInstitution}>
                  <option value="">2. Select Dept (All)</option>
                  {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                </select>
              </div>

              {equipmentList.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead><tr className="border-b border-gray-200"><th className="py-3 px-4 text-sm font-medium text-gray-500">Name & Details</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Institution</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Category</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Status</th><th className="py-3 px-4 text-sm font-medium text-gray-500">Update Status</th></tr></thead>
                    <tbody>
                      {equipmentList.map(eq => (
                        <tr key={eq.id} className="border-b border-gray-100 hover:bg-gray-50">
                          <td className="py-3 px-4"><div className="font-medium text-gray-800">{eq.name}</div><div className="text-xs text-gray-500">SN: {eq.serialNumber} | Mfr: {eq.manufacturer}</div></td>
                          <td className="py-3 px-4 text-gray-600 text-sm">{eq.institutionName}<br/><span className="text-xs text-gray-400">{eq.departmentName}</span></td>
                          <td className="py-3 px-4 text-gray-600 text-sm">{eq.categoryName}</td>
                          <td className="py-3 px-4"><span className={`px-2 py-1 rounded text-xs font-medium ${eq.status === 'AVAILABLE' ? 'bg-green-100 text-green-700' : eq.status === 'BOOKED' ? 'bg-blue-100 text-blue-700' : 'bg-red-100 text-red-700'}`}>{eq.status}</span></td>
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
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="text-center p-8 bg-white rounded-xl border border-dashed border-gray-300 text-gray-500">No equipment found based on filters.</div>
              )}
            </div>
          </section>
        )}

      </main>
    </div>
  );
};

export default Dashboard;
