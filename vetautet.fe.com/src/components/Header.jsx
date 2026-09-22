import { Phone, User, LogOut } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { authService } from '../services/api'

export default function Header() {
  const navigate = useNavigate();
  const userData = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')) : null;

  const handleLogout = () => {
    authService.logout();
    navigate('/auth');
  };

  return (
    <header className="header" id="header">
      <div className="container header-inner" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Link to="/" className="logo">
          <div className="logo-icon">🎫</div>
          <div className="logo-text">
            <span className="logo-name">TICKET PRO</span>
            <span className="logo-tagline">Đặt Vé · Giá Trị Thực</span>
          </div>
        </Link>

        <nav>
          <ul className="nav-list" style={{ display: 'flex', alignItems: 'center', gap: '20px', margin: 0, padding: 0, listStyle: 'none' }}>
            <li><Link to="/" className="nav-link active">Trang Chủ</Link></li>
            <li><Link to="/tickets" className="nav-link">Sự Kiện</Link></li>
            {userData?.role === 'ROLE_ADMIN' && (
              <li><Link to="/system/manager" className="nav-link text-danger">Quản Trị</Link></li>
            )}
          </ul>
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          {userData ? (
            <>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                {userData.avatarUrl ? (
                  <img src={userData.avatarUrl} alt="Avatar" style={{ width: '32px', height: '32px', borderRadius: '50%' }} />
                ) : (
                  <User size={20} />
                )}
                <span style={{ fontSize: '14px', fontWeight: 'bold' }}>{userData.email}</span>
              </div>
              <button onClick={handleLogout} className="btn btn-sm btn-outline-danger" style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                <LogOut size={16} /> Thoát
              </button>
            </>
          ) : (
            <Link to="/auth" className="btn btn-primary" style={{ padding: '8px 16px', borderRadius: '5px', textDecoration: 'none', color: 'white', backgroundColor: '#007bff' }}>
              Đăng nhập
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
