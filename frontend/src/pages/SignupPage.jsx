import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Eye, EyeOff, Loader2, ArrowRight, Building, User } from 'lucide-react';
import api from '../api/axios';

const SignupPage = () => {
  const [activeTab, setActiveTab] = useState('student'); // 'student' or 'institution'
  
  const [studentData, setStudentData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    institutionId: ''
  });

  const [institutionData, setInstitutionData] = useState({
    institutionName: '',
    domain: '',
    address: '',
    contactPhone: '',
    email: '',
    password: ''
  });

  const [institutions, setInstitutions] = useState([]);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const { signupStudent, signupInstitution } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchInstitutions = async () => {
      try {
        const res = await api.get('/institutions/public');
        setInstitutions(res.data);
      } catch (err) {
        console.error("Failed to load institutions", err);
      }
    };
    fetchInstitutions();
  }, []);

  const handleStudentChange = (e) => {
    setStudentData({ ...studentData, [e.target.name]: e.target.value });
  };

  const handleInstitutionChange = (e) => {
    setInstitutionData({ ...institutionData, [e.target.name]: e.target.value });
  };

  const handleStudentSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setIsLoading(true);
    
    try {
      await signupStudent(studentData);
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || 'Student registration failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleInstitutionSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setIsLoading(true);
    
    try {
      const payload = {
        ...institutionData,
        firstName: institutionData.institutionName,
        lastName: 'Admin'
      };
      const res = await signupInstitution(payload);
      setSuccessMsg(res.message || 'Registration successful. Your institution is pending verification by the System Admin.');
      // clear form
      setInstitutionData({
        email: '', password: '',
        institutionName: '', domain: '', address: '', contactPhone: ''
      });
    } catch (err) {
      setError(err.response?.data?.message || 'Institution registration failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-white text-gray-900 font-sans flex flex-col">
      {/* Header */}
      <header className="flex items-center justify-between px-8 py-6">
        <div className="flex items-center gap-2">
          {/* Logo */}
          <img src="/logo.png" alt="Logo" className="w-8 h-8 object-contain" />
          <span className="text-xl font-semibold tracking-tight">Lab Resource Utilization</span>
        </div>
        <Link to="/login" className="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
            <polyline points="10 17 15 12 10 7"></polyline>
            <line x1="15" y1="12" x2="3" y2="12"></line>
          </svg>
          <span className="font-medium text-sm">Sign In</span>
        </Link>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col items-center justify-center px-4 sm:px-6 py-10">
        <div className="w-full max-w-[480px]">
          <h1 className="text-[2.75rem] font-medium tracking-tight text-gray-900 mb-8 text-center">Sign Up</h1>
          
          {/* Tabs */}
          <div className="flex bg-gray-100 p-1 rounded-full mb-8">
            <button
              onClick={() => { setActiveTab('student'); setError(''); setSuccessMsg(''); }}
              className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-full text-sm font-medium transition-colors ${
                activeTab === 'student' ? 'bg-white shadow text-gray-900' : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <User size={18} /> Student
            </button>
            <button
              onClick={() => { setActiveTab('institution'); setError(''); setSuccessMsg(''); }}
              className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-full text-sm font-medium transition-colors ${
                activeTab === 'institution' ? 'bg-white shadow text-gray-900' : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <Building size={18} /> Institution
            </button>
          </div>

          {error && (
            <div className="bg-red-50 text-red-600 p-4 rounded-xl mb-6 text-sm border border-red-100">
              {error}
            </div>
          )}

          {successMsg && (
            <div className="bg-green-50 text-green-700 p-4 rounded-xl mb-6 text-sm border border-green-200 font-medium">
              {successMsg}
            </div>
          )}

          {/* Student Form */}
          {activeTab === 'student' && (
            <form onSubmit={handleStudentSubmit} className="flex flex-col gap-5 animate-fade-in">
              <div className="flex gap-4">
                <div className="flex-1">
                  <input type="text" name="firstName" placeholder="First Name" className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={studentData.firstName} onChange={handleStudentChange} required />
                </div>
                <div className="flex-1">
                  <input type="text" name="lastName" placeholder="Last Name" className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={studentData.lastName} onChange={handleStudentChange} required />
                </div>
              </div>
              <div>
                <input type="email" name="email" placeholder="Email Address" className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={studentData.email} onChange={handleStudentChange} required />
              </div>
              <div className="relative">
                <input type={showPassword ? 'text' : 'password'} name="password" placeholder="Password" className="w-full rounded-full border border-gray-300 pl-6 pr-12 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={studentData.password} onChange={handleStudentChange} required minLength="6" />
                <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors p-1">
                  {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>
              <div>
                <select name="institutionId" className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={studentData.institutionId} onChange={handleStudentChange} required>
                  <option value="" disabled>Select your Institution</option>
                  {institutions.map(inst => (
                    <option key={inst.id} value={inst.id}>{inst.name}</option>
                  ))}
                </select>
              </div>
              <button type="submit" disabled={isLoading} className="w-full bg-gradient-to-r from-[#ff4500] to-[#ff007f] hover:opacity-90 text-white rounded-full py-3.5 font-medium flex items-center justify-center gap-2 transition-opacity disabled:opacity-70 mt-4">
                {isLoading ? <Loader2 size={20} className="animate-spin" /> : <> <ArrowRight size={20} className="mr-1" /> Create Student Account </>}
              </button>
            </form>
          )}

          {/* Institution Form */}
          {activeTab === 'institution' && (
            <form onSubmit={handleInstitutionSubmit} className="flex flex-col gap-5 animate-fade-in">
              <h3 className="font-medium text-gray-700 text-sm uppercase tracking-wider mb-2">Institution Details</h3>
              <div>
                <input type="text" name="institutionName" placeholder="Institution Name" className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={institutionData.institutionName} onChange={handleInstitutionChange} required />
              </div>
              <div>
                <input type="text" name="domain" placeholder="Domain (e.g. mit.edu)" className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={institutionData.domain} onChange={handleInstitutionChange} required />
              </div>
              <div className="flex gap-4">
                <div className="flex-1">
                  <input type="email" name="email" placeholder="Institution Email" className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={institutionData.email} onChange={handleInstitutionChange} required />
                </div>
                <div className="flex-1">
                  <input type="text" name="contactPhone" placeholder="Contact Phone" className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={institutionData.contactPhone} onChange={handleInstitutionChange} required />
                </div>
              </div>
              <div>
                <input type="text" name="address" placeholder="Address" className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={institutionData.address} onChange={handleInstitutionChange} />
              </div>
              <div className="relative">
                <input type={showPassword ? 'text' : 'password'} name="password" placeholder="Password" className="w-full rounded-full border border-gray-300 pl-6 pr-12 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all" value={institutionData.password} onChange={handleInstitutionChange} required minLength="6" />
                <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors p-1">
                  {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>

              <button type="submit" disabled={isLoading} className="w-full bg-gradient-to-r from-[#ff4500] to-[#ff007f] hover:opacity-90 text-white rounded-full py-3.5 font-medium flex items-center justify-center gap-2 transition-opacity disabled:opacity-70 mt-4">
                {isLoading ? <Loader2 size={20} className="animate-spin" /> : <> <ArrowRight size={20} className="mr-1" /> Register Institution </>}
              </button>
            </form>
          )}

        </div>
      </main>
    </div>
  );
};

export default SignupPage;
