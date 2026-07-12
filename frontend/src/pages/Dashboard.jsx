import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Shield, Key, Building, Plus, Loader2, Server, ExternalLink } from 'lucide-react';
import api from '../api/axios';

const Dashboard = () => {
  const { user, logout } = useAuth();
  const [institutions, setInstitutions] = useState([]);
  const [isFetchingInstitutions, setIsFetchingInstitutions] = useState(false);
  
  // User Management State
  const [usersList, setUsersList] = useState([]);
  const [isFetchingUsers, setIsFetchingUsers] = useState(false);
  const [isAssigningRole, setIsAssigningRole] = useState(false);
  
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

  const isSystemAdmin = user?.authorities?.some(auth => auth.authority === 'SYSTEM_ADMIN');

  useEffect(() => {
    if (isSystemAdmin) {
      fetchInstitutions();
      fetchAllUsers();
    }
  }, [isSystemAdmin]);

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

  const handleAssignRole = async (userId, newRole) => {
    setIsAssigningRole(true);
    try {
      await api.post('/users/assign-role', { userId, newRole });
      fetchAllUsers(); // Refresh list to show new role
    } catch (err) {
      alert("Failed to assign role: " + (err.response?.data?.message || err.message));
    } finally {
      setIsAssigningRole(false);
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
              <div className="text-lg font-medium text-gray-800">{user?.username}</div>
            </div>
            
            <div className="bg-gray-50 p-5 rounded-xl border border-gray-100">
              <div className="text-gray-500 text-sm mb-1 flex items-center gap-1.5">
                <Key size={14} /> ID
              </div>
              <div className="text-lg font-medium text-gray-800 font-mono text-sm">{user?.id}</div>
            </div>

            <div className="bg-gray-50 p-5 rounded-xl border border-gray-100 md:col-span-2">
              <div className="text-gray-500 text-sm mb-3 flex items-center gap-1.5">
                <Shield size={16} /> Assigned Roles & Authorities
              </div>
              <div className="flex flex-wrap gap-2">
                {user?.authorities?.map((auth, idx) => (
                  <span key={idx} className="bg-gradient-to-r from-brand-orange to-brand-pink text-white px-4 py-1.5 rounded-full text-xs font-medium shadow-sm">
                    {auth.authority}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* System Admin Features */}
        {isSystemAdmin && (
          <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-medium flex items-center gap-2">
                <Building size={24} className="text-brand-pink" />
                Institution Management
              </h2>
              <button 
                onClick={() => setShowAddForm(!showAddForm)}
                className="bg-gray-900 hover:bg-gray-800 text-white px-4 py-2 rounded-full text-sm font-medium transition-colors flex items-center gap-2"
              >
                <Plus size={16} /> {showAddForm ? 'Cancel' : 'Add Institute'}
              </button>
            </div>

            {/* Add Institution Form */}
            {showAddForm && (
              <form onSubmit={handleAddInstitution} className="bg-gray-50 p-6 rounded-xl border border-gray-200 mb-8 animate-fade-in">
                <h3 className="font-medium text-lg mb-4">Create New Institution</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                  <input
                    type="text" name="name" placeholder="Institution Name" required
                    className="rounded-full border border-gray-300 px-5 py-2.5 focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange outline-none w-full"
                    value={formData.name} onChange={handleChange}
                  />
                  <input
                    type="text" name="domain" placeholder="Domain (e.g. mit.edu)" required
                    className="rounded-full border border-gray-300 px-5 py-2.5 focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange outline-none w-full"
                    value={formData.domain} onChange={handleChange}
                  />
                  <input
                    type="email" name="contactEmail" placeholder="Contact Email" required
                    className="rounded-full border border-gray-300 px-5 py-2.5 focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange outline-none w-full"
                    value={formData.contactEmail} onChange={handleChange}
                  />
                  <input
                    type="text" name="contactPhone" placeholder="Contact Phone" required
                    className="rounded-full border border-gray-300 px-5 py-2.5 focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange outline-none w-full"
                    value={formData.contactPhone} onChange={handleChange}
                  />
                  <input
                    type="text" name="address" placeholder="Physical Address" required
                    className="rounded-full border border-gray-300 px-5 py-2.5 focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange outline-none w-full md:col-span-2"
                    value={formData.address} onChange={handleChange}
                  />
                </div>
                <button 
                  type="submit" 
                  disabled={isSubmitting}
                  className="bg-gradient-to-r from-brand-orange to-brand-pink text-white px-6 py-2.5 rounded-full font-medium flex items-center justify-center gap-2 disabled:opacity-70"
                >
                  {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : 'Save Institution'}
                </button>
              </form>
            )}

            {/* Institutions List */}
            {isFetchingInstitutions ? (
              <div className="flex justify-center p-8"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
            ) : institutions.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-gray-200">
                      <th className="py-3 px-4 text-sm font-medium text-gray-500">Name</th>
                      <th className="py-3 px-4 text-sm font-medium text-gray-500">Domain</th>
                      <th className="py-3 px-4 text-sm font-medium text-gray-500">Contact</th>
                    </tr>
                  </thead>
                  <tbody>
                    {institutions.map(inst => (
                      <tr key={inst.id} className="border-b border-gray-100 hover:bg-gray-50">
                        <td className="py-3 px-4 font-medium text-gray-800">{inst.name}</td>
                        <td className="py-3 px-4 text-gray-600">{inst.domain}</td>
                        <td className="py-3 px-4 text-gray-600">
                          {inst.contactEmail}<br/>
                          <span className="text-xs text-gray-400">{inst.contactPhone}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="text-center p-8 bg-gray-50 rounded-xl border border-dashed border-gray-300 text-gray-500">
                No institutions found. Click "Add Institute" to create one.
              </div>
            )}
          </section>
        )}

        {isSystemAdmin && (
          <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200">
            <div className="mb-6">
              <h2 className="text-xl font-medium flex items-center gap-2">
                <User size={24} className="text-brand-orange" />
                User Management
              </h2>
            </div>
            
            {isFetchingUsers ? (
              <div className="flex justify-center p-8"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
            ) : usersList.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-gray-200">
                      <th className="py-3 px-4 text-sm font-medium text-gray-500">Name</th>
                      <th className="py-3 px-4 text-sm font-medium text-gray-500">Email</th>
                      <th className="py-3 px-4 text-sm font-medium text-gray-500">Current Roles</th>
                      <th className="py-3 px-4 text-sm font-medium text-gray-500">Assign Role</th>
                    </tr>
                  </thead>
                  <tbody>
                    {usersList.map(u => (
                      <tr key={u.id} className="border-b border-gray-100 hover:bg-gray-50">
                        <td className="py-3 px-4 font-medium text-gray-800">{u.firstName} {u.lastName}</td>
                        <td className="py-3 px-4 text-gray-600">{u.email}</td>
                        <td className="py-3 px-4 text-gray-600">
                          <div className="flex flex-wrap gap-1">
                            {u.roles.map(r => (
                              <span key={r} className="bg-gray-200 text-gray-700 px-2 py-0.5 rounded text-xs">
                                {r}
                              </span>
                            ))}
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex gap-2">
                            <select 
                              className="border border-gray-300 rounded px-2 py-1 text-sm bg-white"
                              id={`roleSelect-${u.id}`}
                              defaultValue=""
                            >
                              <option value="" disabled>Select Role...</option>
                              <option value="SYSTEM_ADMIN">System Admin</option>
                              <option value="LAB_MANAGER">Lab Manager</option>
                              <option value="LAB_ASSISTANT">Lab Assistant</option>
                              <option value="RESEARCHER">Researcher</option>
                              <option value="STUDENT">Student</option>
                            </select>
                            <button 
                              disabled={isAssigningRole}
                              onClick={() => {
                                const select = document.getElementById(`roleSelect-${u.id}`);
                                if (select.value) {
                                  handleAssignRole(u.id, select.value);
                                }
                              }}
                              className="bg-brand-orange hover:bg-orange-600 text-white px-3 py-1 rounded text-sm disabled:opacity-70"
                            >
                              Assign
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="text-center p-8 bg-gray-50 rounded-xl border border-dashed border-gray-300 text-gray-500">
                No users found.
              </div>
            )}
          </section>
        )}

      </main>
    </div>
  );
};

export default Dashboard;
