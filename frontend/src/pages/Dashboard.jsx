import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Shield, Key } from 'lucide-react';

const Dashboard = () => {
  const { user, logout } = useAuth();

  return (
    <div className="main-content" style={{ alignItems: 'flex-start', paddingTop: '4rem' }}>
      <div style={{ width: '100%', maxWidth: '800px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <div>
            <h1 className="text-gradient" style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>Dashboard</h1>
            <p style={{ color: 'var(--text-secondary)' }}>Welcome to the Lab Resource Utilization Platform</p>
          </div>
          <button onClick={logout} className="btn-primary" style={{ background: 'rgba(239, 68, 68, 0.2)', color: 'var(--error-color)', padding: '0.5rem 1rem' }}>
            <LogOut size={18} /> Logout
          </button>
        </div>

        <div className="glass-panel animate-fade-in" style={{ padding: '2rem' }}>
          <h2 style={{ fontSize: '1.5rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <User size={24} style={{ color: 'var(--primary-color)' }} />
            User Profile
          </h2>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
            <div style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '1.5rem', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
              <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.5rem' }}>Email Address</div>
              <div style={{ fontSize: '1.125rem', fontWeight: '500' }}>{user?.username}</div>
            </div>
            
            <div style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '1.5rem', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
              <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Key size={14} /> ID
              </div>
              <div style={{ fontSize: '1.125rem', fontWeight: '500' }}>{user?.id}</div>
            </div>

            <div style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '1.5rem', borderRadius: '12px', border: '1px solid var(--border-color)', gridColumn: 'span 2' }}>
              <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Shield size={16} /> Assigned Roles & Authorities
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem' }}>
                {user?.authorities?.map((auth, idx) => (
                  <span key={idx} style={{ 
                    background: 'var(--primary-color)', 
                    padding: '0.5rem 1rem', 
                    borderRadius: '999px', 
                    fontSize: '0.875rem',
                    fontWeight: '500' 
                  }}>
                    {auth.authority}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
