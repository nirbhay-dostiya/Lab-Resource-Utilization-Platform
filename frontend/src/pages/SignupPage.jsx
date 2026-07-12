import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Eye, EyeOff, Loader2, ArrowRight } from 'lucide-react';

const SignupPage = () => {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { signup } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    
    try {
      await signup(formData);
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-white text-gray-900 font-sans flex flex-col">
      {/* Header */}
      <header className="flex items-center justify-between px-8 py-6">
        <div className="flex items-center gap-2">
          {/* Logo Placeholder - Circle Gradient */}
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-brand-pink to-brand-orange"></div>
          <span className="text-xl font-semibold tracking-tight">LabManager</span>
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
      <main className="flex-1 flex flex-col items-center justify-center px-4 sm:px-6 pb-20">
        <div className="w-full max-w-[440px]">
          <h1 className="text-[2.75rem] font-medium tracking-tight text-gray-900 mb-10">Sign Up</h1>
          
          {error && (
            <div className="bg-red-50 text-red-600 p-4 rounded-xl mb-6 text-sm border border-red-100">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="flex flex-col gap-5">
            <div className="flex gap-4">
              {/* First Name Field */}
              <div className="flex-1">
                <input
                  type="text"
                  name="firstName"
                  placeholder="First Name"
                  className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all"
                  value={formData.firstName}
                  onChange={handleChange}
                  required
                />
              </div>

              {/* Last Name Field */}
              <div className="flex-1">
                <input
                  type="text"
                  name="lastName"
                  placeholder="Last Name"
                  className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all"
                  value={formData.lastName}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            {/* Email Field */}
            <div>
              <input
                type="email"
                name="email"
                placeholder="Email Address"
                className="w-full rounded-full border border-gray-300 px-6 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all"
                value={formData.email}
                onChange={handleChange}
                required
              />
            </div>

            {/* Password Field */}
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                name="password"
                placeholder="Password"
                className="w-full rounded-full border border-gray-300 pl-6 pr-12 py-3.5 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-orange/20 focus:border-brand-orange transition-all"
                value={formData.password}
                onChange={handleChange}
                required minLength="6"
              />
              <button 
                type="button" 
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors p-1"
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>

            {/* Submit Button */}
            <button 
              type="submit" 
              disabled={isLoading}
              className="w-full bg-gradient-to-r from-[#ff4500] to-[#ff007f] hover:opacity-90 text-white rounded-full py-3.5 font-medium flex items-center justify-center gap-2 transition-opacity disabled:opacity-70 mt-6"
            >
              {isLoading ? (
                <Loader2 size={20} className="animate-spin" />
              ) : (
                <>
                  <ArrowRight size={20} className="mr-1" /> Create Account
                </>
              )}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
};

export default SignupPage;
