import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/api';
import { GoogleOAuthProvider, GoogleLogin } from '@react-oauth/google';

export default function AuthPage() {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLocalSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (isLogin) {
        const data = await authService.login(email, password);
        if (data) {
          localStorage.setItem('accessToken', data.accessToken);
          localStorage.setItem('refreshToken', data.refreshToken);
          localStorage.setItem('user', JSON.stringify(data));
          navigate('/');
        } else {
          setError('Email hoặc mật khẩu không đúng');
        }
      } else {
        const data = await authService.register(email, password);
        if (data) {
          alert('Đăng ký thành công! Hãy đăng nhập');
          setIsLogin(true);
        }
      }
    } catch (err) {
      setError('Đã xảy ra lỗi kết nối');
    }
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    try {
      const data = await authService.googleLogin(credentialResponse.credential);
      if (data) {
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        localStorage.setItem('user', JSON.stringify(data));
        navigate('/');
      } else {
        setError('Đăng nhập Google thất bại');
      }
    } catch (err) {
      setError('Đã xảy ra lỗi kết nối');
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh', backgroundColor: 'var(--color-bg)' }}>
      <div style={{ 
        width: '100%', 
        maxWidth: '400px', 
        padding: '30px', 
        backgroundColor: 'var(--color-bg-white)', 
        borderRadius: '8px', 
        boxShadow: 'var(--shadow-md)' 
      }}>
        <h2 style={{ textAlign: 'center', color: 'var(--color-primary)', marginBottom: '20px' }}>
          {isLogin ? 'Đăng nhập' : 'Đăng ký tài khoản'}
        </h2>
        
        {error && (
          <div style={{ backgroundColor: 'var(--color-hot)', color: 'white', padding: '10px', borderRadius: '4px', marginBottom: '15px', fontSize: '14px', textAlign: 'center' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleLocalSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '5px', fontSize: '14px', color: 'var(--color-text-secondary)' }}>Email</label>
            <input 
              type="email" 
              value={email} 
              onChange={(e) => setEmail(e.target.value)} 
              required 
              style={{ width: '100%', padding: '10px', border: '1px solid var(--color-border)', borderRadius: '4px', fontSize: '14px', boxSizing: 'border-box' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '5px', fontSize: '14px', color: 'var(--color-text-secondary)' }}>Mật khẩu</label>
            <input 
              type="password" 
              value={password} 
              onChange={(e) => setPassword(e.target.value)} 
              required 
              style={{ width: '100%', padding: '10px', border: '1px solid var(--color-border)', borderRadius: '4px', fontSize: '14px', boxSizing: 'border-box' }}
            />
          </div>
          <button type="submit" style={{ 
            width: '100%', padding: '12px', backgroundColor: 'var(--color-primary)', color: 'white', 
            border: 'none', borderRadius: '4px', fontSize: '16px', fontWeight: 'bold', marginTop: '10px' 
          }}>
            {isLogin ? 'Đăng nhập' : 'Đăng ký'}
          </button>
        </form>

        <div style={{ display: 'flex', alignItems: 'center', margin: '20px 0', color: 'var(--color-text-light)' }}>
          <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--color-border)' }}></div>
          <span style={{ padding: '0 10px', fontSize: '14px' }}>Hoặc</span>
          <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--color-border)' }}></div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '20px' }}>
          <GoogleOAuthProvider clientId="598903688201-tmhrd1s1buk0o75ti0fi4sk2jfmusi8s.apps.googleusercontent.com">
            <GoogleLogin
              onSuccess={handleGoogleSuccess}
              onError={() => setError('Đăng nhập Google bị hủy')}
              theme="outline"
              size="large"
              width="340"
            />
          </GoogleOAuthProvider>
        </div>

        <div style={{ textAlign: 'center' }}>
          <button 
            type="button"
            onClick={() => { setIsLogin(!isLogin); setError(''); }}
            style={{ background: 'none', border: 'none', color: 'var(--color-primary-light)', fontSize: '14px', textDecoration: 'underline', padding: 0 }}
          >
            {isLogin ? 'Chưa có tài khoản? Đăng ký ngay' : 'Đã có tài khoản? Đăng nhập'}
          </button>
        </div>
      </div>
    </div>
  );
}
