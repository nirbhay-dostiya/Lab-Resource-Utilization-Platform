import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Shield, Key, Building, Plus, Loader2, Server, ExternalLink, Network, Tags, Settings, ChevronDown, ChevronUp, Calendar, MoreVertical, LayoutDashboard, Activity, CheckCircle, Wrench, Clock, FileText, ShoppingCart, X, Bell, Search } from 'lucide-react';
import { ComposedChart, BarChart, Bar, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import api from '../api/axios';
import toast from 'react-hot-toast';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }
  static getDerivedStateFromError(error) {
    return { hasError: true };
  }
  componentDidCatch(error, errorInfo) {
    this.setState({ error, errorInfo });
    console.error("ErrorBoundary caught an error", error, errorInfo);
  }
  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: '2rem', background: '#fee', color: 'red', border: '2px solid red', margin: '2rem', borderRadius: '8px' }}>
          <h1 style={{ fontSize: '24px', fontWeight: 'bold' }}>Something went wrong.</h1>
          <p style={{ marginTop: '1rem', fontWeight: 'bold' }}>{this.state.error && this.state.error.toString()}</p>
          <pre style={{ marginTop: '1rem', overflow: 'auto', whiteSpace: 'pre-wrap', fontSize: '12px' }}>
            {this.state.errorInfo && this.state.errorInfo.componentStack}
          </pre>
        </div>
      );
    }
    return this.props.children;
  }
}

const Dashboard = () => {
  const { user, logout } = useAuth();

  const hasRole = (roleName) => user?.roles?.includes(roleName) || user?.authorities?.includes(roleName) || user?.authorities?.some(auth => auth.authority === roleName);
  const isSystemAdmin = user?.roles?.includes('SYSTEM_ADMIN') || user?.authorities?.some(auth => auth.authority === 'SYSTEM_ADMIN');
  const canManage = isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER');

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
  const [selectedEquipmentDetails, setSelectedEquipmentDetails] = useState(null);
  const [showWaitlistModal, setShowWaitlistModal] = useState(false);
  const [selectedWaitlistDetails, setSelectedWaitlistDetails] = useState(null);
  const [bookingData, setBookingData] = useState({ equipmentId: '', equipmentName: '', startTime: '', endTime: '', purpose: '' });
  const [bookingTab, setBookingTab] = useState('book'); // 'book', 'history', or 'waitlists'
  const [waitlistsList, setWaitlistsList] = useState([]);
  const [isFetchingWaitlists, setIsFetchingWaitlists] = useState(false);
  const [equipmentManagementTab, setEquipmentManagementTab] = useState('add'); // 'add' or 'view'
  const [equipmentPage, setEquipmentPage] = useState(1);
  const [myEquipmentPage, setMyEquipmentPage] = useState(1);
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

  const [editingEquipment, setEditingEquipment] = useState(null);
  const [showEditEquipmentModal, setShowEditEquipmentModal] = useState(false);

  // Utilization & Shared Resources State
  const [heatmapData, setHeatmapData] = useState([]);
  const [performanceData, setPerformanceData] = useState({ top: [], bottom: [] });
  const [isFetchingHeatmap, setIsFetchingHeatmap] = useState(false);
  const [isFetchingPerformance, setIsFetchingPerformance] = useState(false);
  const [sharedResources, setSharedResources] = useState([]);
  const [myRequests, setMyRequests] = useState([]);
  const [isFetchingShared, setIsFetchingShared] = useState(false);
  const [accessRequestData, setAccessRequestData] = useState({ listingId: '', equipmentName: '', justification: '', requestedStart: '', requestedEnd: '' });
  const [showAccessModal, setShowAccessModal] = useState(false);
  const [occupiedDates, setOccupiedDates] = useState([]);
  const [isFetchingOccupiedDates, setIsFetchingOccupiedDates] = useState(false);

  // Share Equipment State
  const [showShareModal, setShowShareModal] = useState(false);
  const [shareData, setShareData] = useState({ equipmentId: '', equipmentName: '', termsAndConditions: '', availableFrom: '', availableTo: '' });

  const [incomingRequests, setIncomingRequests] = useState([]);
  const [isFetchingIncoming, setIsFetchingIncoming] = useState(false);
  const [selectedIncomingRequest, setSelectedIncomingRequest] = useState(null);

  // Milestone 3 States
  const [maintenanceTasks, setMaintenanceTasks] = useState([]);
  const [calibrationRecords, setCalibrationRecords] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [budgets, setBudgets] = useState([]);
  const [analyticsData, setAnalyticsData] = useState(null);

  // Analytics Drill-Down Modal States
  const [showAnalyticsDetailsModal, setShowAnalyticsDetailsModal] = useState(false);
  const [analyticsDetailsType, setAnalyticsDetailsType] = useState(''); // 'equipment' or 'bookings'
  const [analyticsDetailsTitle, setAnalyticsDetailsTitle] = useState('');
  const [analyticsDetailsStatus, setAnalyticsDetailsStatus] = useState(null);
  const [analyticsDetailsData, setAnalyticsDetailsData] = useState([]);
  const [analyticsDetailsPage, setAnalyticsDetailsPage] = useState(0);
  const [analyticsDetailsTotalPages, setAnalyticsDetailsTotalPages] = useState(0);
  const [isFetchingAnalyticsDetails, setIsFetchingAnalyticsDetails] = useState(false);

  // Dashboard KPI Drill-Down Modal States
  const [showKpiModal, setShowKpiModal] = useState(false);
  const [kpiModalTitle, setKpiModalTitle] = useState('');
  const [kpiModalData, setKpiModalData] = useState([]);
  const [kpiModalType, setKpiModalType] = useState(''); // 'equipment' | 'bookings' | 'maintenance' | 'pending'
  const [kpiModalAuthorized, setKpiModalAuthorized] = useState(true);

  // Bookings filter & pagination state
  const [bookingFilterStatus, setBookingFilterStatus] = useState('ALL');
  const [bookingSearchQuery, setBookingSearchQuery] = useState('');
  const [bookingCurrentPage, setBookingCurrentPage] = useState(0);
  const BOOKING_PAGE_SIZE = 10;
  const [bookingHistoryInstitutionFilter, setBookingHistoryInstitutionFilter] = useState('');
  const [bookingHistorySearchFilter, setBookingHistorySearchFilter] = useState('');
  const [bookingHistoryStatusFilter, setBookingHistoryStatusFilter] = useState('');

  const [notifications, setNotifications] = useState([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [selectedNotification, setSelectedNotification] = useState(null);
  const notificationRef = useRef(null);

  // Payment Simulation States
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [selectedInvoiceToPay, setSelectedInvoiceToPay] = useState(null);
  const [paymentData, setPaymentData] = useState({ paymentMethod: 'CREDIT_CARD', referenceNumber: '' });

  const [invoiceSearchQuery, setInvoiceSearchQuery] = useState('');
  const [selectedInvoiceDetails, setSelectedInvoiceDetails] = useState(null);
  const [isInvoiceViewOpen, setIsInvoiceViewOpen] = useState(false);

  // Billing pagination & filter state
  const [invoiceCurrentPage, setInvoiceCurrentPage] = useState(0);
  const [invoiceStatusFilter, setInvoiceStatusFilter] = useState('ALL');
  const [billingInstitutionFilter, setBillingInstitutionFilter] = useState('');
  const [txCurrentPage, setTxCurrentPage] = useState(0);
  const INVOICE_PAGE_SIZE = 10;
  const TX_PAGE_SIZE = 10;
  const [selectedTransactionDetails, setSelectedTransactionDetails] = useState(null);
  const [isTransactionModalOpen, setIsTransactionModalOpen] = useState(false);

  // â”€â”€ Module 1: Work Order State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const [workOrderForm, setWorkOrderForm] = useState({
    equipmentId: '', taskType: 'PREVENTIVE', priority: 'MEDIUM',
    description: '', scheduledDate: '', technicianId: ''
  });
  const [isSubmittingWorkOrder, setIsSubmittingWorkOrder] = useState(false);
  const [workOrderStatusFilter, setWorkOrderStatusFilter] = useState('ALL');
  const [showWorkOrderForm, setShowWorkOrderForm] = useState(false);
  const [workOrderTransition, setWorkOrderTransition] = useState({ id: null, currentStatus: '', targetStatus: '', notes: '', downtimeHours: '' });
  const [showTransitionModal, setShowTransitionModal] = useState(false);

  // â”€â”€ Module 2: Calibration Compliance State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const [complianceDashboard, setComplianceDashboard] = useState([]);
  const [isFetchingCompliance, setIsFetchingCompliance] = useState(false);
  const [calibrationForm, setCalibrationForm] = useState({
    equipmentId: '', calibrationDate: '', expiryDate: '',
    vendorName: '', status: 'VALID', certificateUrl: '', toleranceMetrics: ''
  });
  const [showCalibrationForm, setShowCalibrationForm] = useState(false);
  const [isSubmittingCalibration, setIsSubmittingCalibration] = useState(false);

  // â”€â”€ Module 3: Billing Approval State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const [isApprovingInvoice, setIsApprovingInvoice] = useState(false);
  const [isSubmittingForApproval, setIsSubmittingForApproval] = useState(false);
  const [autoInvoiceForm, setAutoInvoiceForm] = useState({
    periodStart: '', periodEnd: '', fundingSourceId: ''
  });
  const [showAutoInvoiceForm, setShowAutoInvoiceForm] = useState(false);
  const [fundingSources, setFundingSources] = useState([]);

  // â”€â”€ Module 4: Persona Analytics State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const [researcherDashboard, setResearcherDashboard] = useState(null);
  const [labManagerDashboard, setLabManagerDashboard] = useState(null);
  const [systemAdminDashboard, setSystemAdminDashboard] = useState(null);
  const [isFetchingPersonaDashboard, setIsFetchingPersonaDashboard] = useState(false);

  // â”€â”€ Module 5: OEE Report State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const [reportForm, setReportForm] = useState({ from: '', to: '' });
  const [reportId, setReportId] = useState(null);
  const [reportStatus, setReportStatus] = useState('');
  const [reportResult, setReportResult] = useState(null);
  const [isGeneratingReport, setIsGeneratingReport] = useState(false);
  const [activeReportTab, setActiveReportTab] = useState('generate'); // 'generate' | 'results'
  const reportPollRef = useRef(null);

  // â”€â”€ New enterprise fetch functions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  const fetchComplianceDashboard = async () => {
    setIsFetchingCompliance(true);
    try {
      const res = await api.get('/calibration/compliance-dashboard');
      setComplianceDashboard(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Failed to fetch compliance dashboard', err);
    } finally {
      setIsFetchingCompliance(false);
    }
  };

  const fetchFundingSources = async () => {
    try {
      const res = await api.get('/billing/funding-sources');
      setFundingSources(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Failed to fetch funding sources', err);
    }
  };

  const fetchPersonaDashboard = async () => {
    setIsFetchingPersonaDashboard(true);
    try {
      if (isSystemAdmin) {
        const res = await api.get('/analytics/dashboard/system-admin');
        setSystemAdminDashboard(res.data);
      } else if (hasRole('LAB_MANAGER') || hasRole('DEPT_HEAD') || hasRole('INSTITUTION_ADMIN')) {
        if (selectedInstitution) {
          const res = await api.get(`/analytics/dashboard/lab-manager?institutionId=${selectedInstitution}`);
          setLabManagerDashboard(res.data);
        }
      } else if (user?.id) {
        const res = await api.get(`/analytics/dashboard/researcher?userId=${user.id}`);
        setResearcherDashboard(res.data);
      }
    } catch (err) {
      console.error('Failed to fetch persona dashboard', err);
    } finally {
      setIsFetchingPersonaDashboard(false);
    }
  };

  const handleGenerateReport = async () => {
    if (!reportForm.from || !reportForm.to) { alert('Please select a date range.'); return; }
    setIsGeneratingReport(true);
    setReportStatus('PENDING');
    setReportResult(null);
    try {
      const res = await api.post(`/reports/generate?from=${reportForm.from}&to=${reportForm.to}`);
      const rid = res.data.reportId;
      setReportId(rid);
      // Poll every 2 seconds until DONE or FAILED
      reportPollRef.current = setInterval(async () => {
        try {
          const s = await api.get(`/reports/${rid}/status`);
          setReportStatus(s.data.status);
          if (s.data.status === 'DONE') {
            clearInterval(reportPollRef.current);
            setIsGeneratingReport(false);
            setActiveReportTab('results');
            // Fetch the already-computed result (does NOT re-trigger generation)
            const result = await api.get(`/reports/${rid}/result`);
            setReportResult(result.data);
          } else if (s.data.status === 'FAILED') {
            clearInterval(reportPollRef.current);
            setIsGeneratingReport(false);
            alert('Report generation failed. Please try again.');
          }
        } catch (pollErr) { clearInterval(reportPollRef.current); setIsGeneratingReport(false); }
      }, 2000);
    } catch (err) {
      setIsGeneratingReport(false);
      alert('Failed to start report: ' + (err.response?.data?.message || err.message));
    }
  };


  const handleSubmitWorkOrder = async () => {
    if (!workOrderForm.equipmentId || !workOrderForm.scheduledDate) {
      alert('Equipment and scheduled date are required.'); return;
    }
    setIsSubmittingWorkOrder(true);
    try {
      await api.post('/maintenance', {
        equipmentId: workOrderForm.equipmentId,
        maintenanceType: workOrderForm.taskType,
        priority: workOrderForm.priority,
        description: workOrderForm.description,
        scheduledDate: workOrderForm.scheduledDate + 'T08:00:00',
        technicianId: workOrderForm.technicianId || null,
      });
      setShowWorkOrderForm(false);
      setWorkOrderForm({ equipmentId: '', taskType: 'PREVENTIVE', priority: 'MEDIUM', description: '', scheduledDate: '', technicianId: '' });
      fetchMaintenanceTasks();
      alert('Work order created successfully!');
    } catch (err) {
      alert('Failed to create work order: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsSubmittingWorkOrder(false);
    }
  };

  const handleWorkOrderTransition = async () => {
    if (!workOrderTransition.id || !workOrderTransition.targetStatus) return;
    try {
      await api.patch(`/maintenance/${workOrderTransition.id}/transition?targetStatus=${workOrderTransition.targetStatus}` +
        (workOrderTransition.notes ? `&resolutionNotes=${encodeURIComponent(workOrderTransition.notes)}` : '') +
        (workOrderTransition.downtimeHours ? `&downtimeHours=${workOrderTransition.downtimeHours}` : ''));
      setShowTransitionModal(false);
      setWorkOrderTransition({ id: null, targetStatus: '', notes: '', downtimeHours: '' });
      fetchMaintenanceTasks();
    } catch (err) {
      alert('Transition failed: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleLogCalibration = async () => {
    if (!calibrationForm.equipmentId || !calibrationForm.calibrationDate || !calibrationForm.expiryDate) {
      alert('Equipment, calibration date, and expiry date are required.'); return;
    }
    setIsSubmittingCalibration(true);
    try {
      await api.post('/calibration', {
        equipmentId: calibrationForm.equipmentId,
        calibrationDate: calibrationForm.calibrationDate,
        expiryDate: calibrationForm.expiryDate,
        vendorName: calibrationForm.vendorName || null,
        status: calibrationForm.status,
        certificateUrl: calibrationForm.certificateUrl || null,
        toleranceMetrics: calibrationForm.toleranceMetrics || null,
      });
      setShowCalibrationForm(false);
      setCalibrationForm({ equipmentId: '', calibrationDate: '', expiryDate: '', vendorName: '', status: 'VALID', certificateUrl: '', toleranceMetrics: '' });
      fetchCalibrationRecords();
      fetchComplianceDashboard();
      alert('Calibration record logged successfully!');
    } catch (err) {
      alert('Failed to log calibration: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsSubmittingCalibration(false);
    }
  };

  const handleGenerateAutoInvoice = async () => {
    if (!selectedDepartment || !autoInvoiceForm.periodStart || !autoInvoiceForm.periodEnd) {
      alert('Select a department and billing period.'); return;
    }
    try {
      const params = new URLSearchParams({
        departmentId: selectedDepartment,
        periodStart: autoInvoiceForm.periodStart,
        periodEnd: autoInvoiceForm.periodEnd,
        ...(autoInvoiceForm.fundingSourceId ? { fundingSourceId: autoInvoiceForm.fundingSourceId } : {})
      });
      await api.post(`/billing/invoices/generate-auto?${params}`);
      setShowAutoInvoiceForm(false);
      fetchInvoices();
      alert('Auto-invoice generated successfully!');
    } catch (err) {
      alert('Failed: ' + (err.response?.data?.message || err.message));
    }
  };

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
      if (activeSection === 'equipment') {
        fetchSharedResources();
      }
    }
    if (activeSection === 'bookings' || activeSection === 'book_equipment') {
      fetchBookings();
      fetchWaitlists();
      // Fetch equipment for stats if not already loaded or if Admin/Inst Admin
      if (equipmentList.length === 0) {
        if (selectedDepartment) fetchEquipment('department', selectedDepartment);
        else if (selectedInstitution) fetchEquipment('institution', selectedInstitution);
        else fetchEquipment('global');
      }
    }
    if (activeSection === 'utilization') {
      fetchHeatmapData();
      fetchPerformanceData();
    }
    if (activeSection === 'shared_resources') {
      fetchSharedResources();
      fetchMyRequests();
      const canManageRequests = user?.roles?.some(r => ['INSTITUTION_ADMIN', 'DEPT_HEAD', 'LAB_MANAGER'].includes(r));
      if (canManageRequests) {
        fetchIncomingRequests();
      }
    }
    if (activeSection === 'maintenance') {
      fetchMaintenanceTasks();
      fetchCalibrationRecords();
      if (canManage) fetchComplianceDashboard();
    }
    if (activeSection === 'billing') {
      fetchInvoices();
      fetchFundingSources();
      if (selectedDepartment) fetchBudgets();
    }
    if (activeSection === 'analytics') {
      fetchAnalyticsData();
      fetchPersonaDashboard();
    }
    if (activeSection === 'reports') {
      // No auto-fetch â€” user initiates report generation
    }
  }, [activeSection, isSystemAdmin, selectedDepartment, selectedInstitution, equipmentList.length]);


  const fetchMaintenanceTasks = async () => {
    try {
      const res = await api.get('/maintenance');
      setMaintenanceTasks(res.data);
    } catch (err) {
      console.error("Failed to fetch maintenance tasks", err);
    }
  };

  const fetchCalibrationRecords = async () => {
    try {
      const res = await api.get('/calibration');
      setCalibrationRecords(res.data);
    } catch (err) {
      console.error("Failed to fetch calibration records", err);
    }
  };

  const fetchInvoices = async () => {
    try {
      let endpoint = '/billing/invoices';
      if (selectedDepartment) {
        endpoint = `/billing/invoices/department/${selectedDepartment}`;
      } else if (!isSystemAdmin) {
        endpoint = '/billing/invoices/my-institution';
      }
      const res = await api.get(endpoint);
      setInvoices(res.data);
    } catch (err) {
      console.error("Failed to fetch invoices", err);
    }
  };

  const handlePaymentSubmit = async (status) => {
    if (!selectedInvoiceToPay) return;
    try {
      const payload = {
        amount: selectedInvoiceToPay.totalAmount,
        paymentMethod: paymentData.paymentMethod,
        referenceNumber: paymentData.referenceNumber || 'SIM-' + Date.now(),
        status: status // 'SUCCESS' or 'FAILED'
      };
      await api.post(`/billing/invoices/${selectedInvoiceToPay.id}/pay`, payload);

      if (status === 'SUCCESS') {
        alert('Payment simulated successfully!');
      } else {
        alert('Payment failed simulation!');
      }
      setIsPaymentModalOpen(false);
      setSelectedInvoiceToPay(null);
      fetchInvoices();
      fetchNotifications();
      if (selectedDepartment) fetchBudgets();
      if (activeSection === 'bookings' || activeSection === 'book_equipment') fetchBookings();
    } catch (err) {
      console.error('Payment processing error', err);
      alert(err.response?.data?.message || 'An error occurred during payment processing.');
    }
  };

  const fetchBudgets = async () => {
    if (!selectedDepartment) return;
    try {
      const res = await api.get(`/budgets/department/${selectedDepartment}`);
      setBudgets(res.data);
    } catch (err) {
      console.error("Failed to fetch budgets", err);
    }
  };

  const filteredInvoices = (Array.isArray(invoices) ? invoices : []).filter(inv => {
    // Institution filter (system admin only)
    if (billingInstitutionFilter && inv?.billedToInstitutionName !== billingInstitutionFilter) return false;
    // Status filter
    if (invoiceStatusFilter !== 'ALL' && inv?.status !== invoiceStatusFilter) return false;
    // Text search
    if (!invoiceSearchQuery) return true;
    const q = invoiceSearchQuery.toLowerCase();
    return (
      inv?.id?.toLowerCase()?.includes(q) ||
      inv?.lineItems?.some(li => li?.equipmentName?.toLowerCase()?.includes(q) || li?.description?.toLowerCase()?.includes(q)) ||
      inv?.userEmail?.toLowerCase()?.includes(q) ||
      inv?.userName?.toLowerCase()?.includes(q)
    );
  });

  // All transactions across all filtered invoices
  const allTransactions = filteredInvoices
    .flatMap(inv => (Array.isArray(inv?.transactions) ? inv.transactions : []).map(t => ({ ...t, invoiceId: inv?.id, invoiceAmount: inv?.totalAmount })))
    .sort((a, b) => new Date(b?.transactionDate || 0) - new Date(a?.transactionDate || 0));

  const fetchAnalyticsData = async () => {
    try {
      let endpoint = '';
      if (selectedDepartment) {
        endpoint = `/analytics/department/${selectedDepartment}`;
      } else if (selectedInstitution) {
        endpoint = `/analytics/institution/${selectedInstitution}`;
      } else if (isSystemAdmin) {
        endpoint = '/analytics/global';
      } else {
        setAnalyticsData({ error: 'No institution or department selected for analytics.' });
        return;
      }
      const res = await api.get(endpoint);
      setAnalyticsData(res.data);
    } catch (err) {
      console.error("Failed to fetch analytics data", err);
      setAnalyticsData({ error: err.response?.data?.message || err.message || 'Failed to fetch analytics data. Please check permissions.' });
    }
  };

  const fetchAnalyticsDetails = async (type, status, page = 0) => {
    try {
      setIsFetchingAnalyticsDetails(true);
      let endpoint = '';
      if (selectedDepartment) {
        endpoint = `/analytics/department/${selectedDepartment}/${type}?page=${page}&size=10`;
      } else if (selectedInstitution) {
        endpoint = `/analytics/institution/${selectedInstitution}/${type}?page=${page}&size=10`;
      } else if (isSystemAdmin) {
        endpoint = `/analytics/global/${type}?page=${page}&size=10`;
      } else {
        return;
      }
      if (status) {
        endpoint += `&status=${status}`;
      }
      
      const res = await api.get(endpoint);
      setAnalyticsDetailsData(res.data.content);
      setAnalyticsDetailsTotalPages(res.data.totalPages);
      setAnalyticsDetailsPage(res.data.number);
    } catch (err) {
      console.error("Failed to fetch analytics details", err);
      toast.error(err.response?.data?.message || "Failed to fetch details.");
    } finally {
      setIsFetchingAnalyticsDetails(false);
    }
  };

  const openAnalyticsDetails = (title, type, status) => {
    setAnalyticsDetailsTitle(title);
    setAnalyticsDetailsType(type);
    setAnalyticsDetailsStatus(status);
    setAnalyticsDetailsPage(0);
    setShowAnalyticsDetailsModal(true);
    fetchAnalyticsDetails(type, status, 0);
  };

  const toastedNotificationIds = useRef(new Set());

  // SSE Notification Pipeline
  useEffect(() => {
    if (!user?.id) return;

    // Initial fetch to load existing unread notifications
    const fetchInitialNotifications = async () => {
      try {
        const res = await api.get(`/notifications/user/${user.id}/unread`);
        setNotifications(res.data || []);
      } catch (err) {
        console.error("Failed to fetch initial notifications", err);
      }
    };
    fetchInitialNotifications();

    // Establish real-time SSE connection
    const token = localStorage.getItem('token');
    const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
    const eventSource = new EventSource(`${baseUrl}/notifications/stream/${user.id}?token=${token}`);

    eventSource.addEventListener("notification", (event) => {
      try {
        const newNotification = JSON.parse(event.data);
        // Use functional state update to prevent race conditions
        setNotifications(prev => {
          // Prevent duplicates in state
          if (prev.find(n => n.id === newNotification.id)) return prev;
          return [newNotification, ...prev];
        });

        // Outside of the pure updater, trigger side effects safely using a ref to deduplicate
        if (!toastedNotificationIds.current.has(newNotification.id)) {
          toastedNotificationIds.current.add(newNotification.id);
          
          setTimeout(() => {
             toast(
               (t) => (
                 <div className="flex flex-col gap-1 cursor-pointer" onClick={() => toast.dismiss(t.id)}>
                   <div className="font-semibold text-sm text-gray-800">
                     {newNotification.actorName ? (
                       <span>
                         <span className="text-blue-600">{newNotification.actorName}</span>{' '}
                         <span className="text-xs uppercase text-gray-500 tracking-wide">({newNotification.action ? newNotification.action.replace(/_/g, ' ') : newNotification.title})</span>
                       </span>
                     ) : (
                       newNotification.title || newNotification.referenceType
                     )}
                   </div>
                   <div className="text-xs text-gray-600 line-clamp-2">{newNotification.message || newNotification.content}</div>
                 </div>
               ),
               { duration: 5000, position: 'top-right' }
             );
          }, 0);
        }

      } catch (err) {
        console.error("Failed to parse incoming SSE notification", err);
      }
    });

    eventSource.onerror = (error) => {
      console.error("SSE connection error", error);
      eventSource.close();
      // Polling fallback could be implemented here if SSE drops
    };

    return () => {
      eventSource.close();
    };
  }, [user]);

  // Close notification panel when clicking outside
  useEffect(() => {
    if (!showNotifications) return;
    const handleClickOutside = (e) => {
      if (notificationRef.current && !notificationRef.current.contains(e.target)) {
        setShowNotifications(false);
      }
    };
    // Use capture phase so clicks on other interactive elements are caught first
    document.addEventListener('mousedown', handleClickOutside, true);
    return () => document.removeEventListener('mousedown', handleClickOutside, true);
  }, [showNotifications]);


  const dashboardStats = useMemo(() => {
    const today = new Date().toDateString();
    let pendingCount = bookingsList.filter(b => b.status === 'PENDING').length;

    if (isSystemAdmin) {
      pendingCount += institutions.filter(inst => !(inst.isActive || inst.active)).length;
    }

    // Filter equipment to only show the user's institution, unless they are a System Admin
    const relevantEquipment = isSystemAdmin ? equipmentList : equipmentList.filter(e => e.institutionId === user?.institutionId);

    return {
      totalEquipment: relevantEquipment.length,
      todaysBookings: bookingsList.filter(b => new Date(b.startTime).toDateString() === today).length,
      pendingApprovals: pendingCount,
      underMaintenance: relevantEquipment.filter(e => e.status === 'UNDER_MAINTENANCE').length,
    };
  }, [equipmentList, bookingsList, institutions, isSystemAdmin, user]);

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

  const fetchWaitlists = async () => {
    if (!user?.id) return;
    setIsFetchingWaitlists(true);
    try {
      const res = await api.get(`/v1/waitlists/user/${user.id}`);
      setWaitlistsList(res.data);
    } catch (err) {
      console.error("Failed to fetch waitlists", err);
    } finally {
      setIsFetchingWaitlists(false);
    }
  };

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

  const fetchHeatmapData = async () => {
    setIsFetchingHeatmap(true);
    try {
      // Use local date (not UTC) to avoid IST +05:30 shifting the date window
      const toLocalDateStr = (d) => {
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
      };
      const end = new Date();
      const start = new Date();
      start.setDate(end.getDate() - 7);
      const res = await api.get(`/v1/utilization-analytics/heatmap?startDate=${toLocalDateStr(start)}&endDate=${toLocalDateStr(end)}`);
      if (Array.isArray(res.data)) {
        setHeatmapData(res.data);
      } else {
        setHeatmapData([]);
      }
    } catch (err) {
      console.error("Failed to fetch heatmap data", err);
      setHeatmapData([]);
    } finally {
      setIsFetchingHeatmap(false);
    }
  };

  const fetchPerformanceData = async () => {
    setIsFetchingPerformance(true);
    try {
      // Use local date (not UTC) to avoid IST +05:30 shifting the date window
      const toLocalDateStr = (d) => {
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
      };
      const end = new Date();
      const start = new Date();
      start.setDate(end.getDate() - 7);
      const res = await api.get(`/v1/utilization-analytics/performance?startDate=${toLocalDateStr(start)}&endDate=${toLocalDateStr(end)}`);
      if (res.data) {
        setPerformanceData(res.data);
      }
    } catch (err) {
      console.error("Failed to fetch performance data", err);
    } finally {
      setIsFetchingPerformance(false);
    }
  };

  
  const fetchOccupiedDates = async (listingId) => {
    setIsFetchingOccupiedDates(true);
    try {
      const response = await api.get(`/resource-sharing/listings/${listingId}/availability`);
      setOccupiedDates(response.data);
    } catch (err) {
      console.error("Failed to fetch availability", err);
      toast.error("Could not fetch equipment availability.");
    } finally {
      setIsFetchingOccupiedDates(false);
    }
  };

  const fetchSharedResources = async () => {
    setIsFetchingShared(true);
    try {
      const res = await api.get('/v1/resource-sharing/listings/active');
      setSharedResources(res.data);
    } catch (err) {
      console.error("Failed to fetch shared resources", err);
    } finally {
      setIsFetchingShared(false);
    }
  };

  const fetchMyRequests = async () => {
    if (!user?.id) return;
    try {
      const res = await api.get(`/v1/resource-sharing/requests/requester/${user.id}`);
      setMyRequests(res.data);
    } catch (err) {
      console.error("Failed to fetch my requests", err);
    }
  };

  const fetchIncomingRequests = async () => {
    if (!user?.institutionId) return;
    setIsFetchingIncoming(true);
    try {
      const res = await api.get(`/v1/resource-sharing/requests/institution/${user.institutionId}`);
      setIncomingRequests(res.data);
    } catch (err) {
      console.error("Failed to fetch incoming requests", err);
    } finally {
      setIsFetchingIncoming(false);
    }
  };

  const handleApproveRejectRequest = async (requestId, status) => {
    // Save original state in case we need to revert
    const originalRequests = [...incomingRequests];

    // 1. Optimistically update UI instantly BEFORE the network request
    setIncomingRequests(prev =>
      prev.map(req => req.id === requestId ? { ...req, status } : req)
    );

    try {
      // 2. Make the API call in the background
      await api.put(`/v1/resource-sharing/requests/${requestId}/status?status=${status}&approverId=${user.id}`);
      toast.success(`Request ${status === 'APPROVED' ? 'approved' : 'rejected'} successfully`);
    } catch (err) {
      // 3. If the network request fails, revert the UI and show error
      console.error(`Failed to ${status.toLowerCase()} request`, err);
      toast.error(`Failed to update request`);
      setIncomingRequests(originalRequests);
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

    const imageFile = formData.get('imageFile');
    if (imageFile && imageFile.size > 0) {
      try {
        const base64Image = await new Promise((resolve, reject) => {
          const reader = new FileReader();
          reader.onloadend = () => resolve(reader.result);
          reader.onerror = reject;
          reader.readAsDataURL(imageFile);
        });
        data.imageBase64 = base64Image;
      } catch (err) {
        alert("Failed to read image file.");
        return;
      }
    }
    delete data.imageFile;

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

  const handleEditEquipmentSubmit = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);

    const imageFile = formData.get('imageFile');
    if (imageFile && imageFile.size > 0) {
      try {
        const base64Image = await new Promise((resolve, reject) => {
          const reader = new FileReader();
          reader.onloadend = () => resolve(reader.result);
          reader.onerror = reject;
          reader.readAsDataURL(imageFile);
        });
        data.imageBase64 = base64Image;
      } catch (err) {
        alert("Failed to read image file.");
        return;
      }
    } else {
      // Keep existing image if no new one is provided
      data.imageBase64 = editingEquipment.imageBase64;
    }
    delete data.imageFile;

    try {
      await api.put(`/equipment/${editingEquipment.id}`, data);
      alert("Equipment updated successfully!");
      setShowEditEquipmentModal(false);
      setEditingEquipment(null);
      if (selectedDepartment) {
        fetchEquipment('department', selectedDepartment);
      } else if (selectedInstitution) {
        fetchEquipment('institution', selectedInstitution);
      } else {
        fetchEquipment('global');
      }
    } catch (err) {
      alert("Failed to update equipment: " + (err.response?.data?.message || err.message));
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
      const res = await api.post('/bookings', {
        equipmentId: bookingData.equipmentId,
        startTime: bookingData.startTime,
        endTime: bookingData.endTime,
        purpose: bookingData.purpose
      });
      setShowBookingModal(false);

      if (res.data && res.data.invoiceId) {
        setSelectedInvoiceToPay({ id: res.data.invoiceId, totalAmount: res.data.totalAmount });
        setIsPaymentModalOpen(true);
        if (activeSection === 'bookings' || activeSection === 'book_equipment') fetchBookings();
      } else {
        alert("Booking request submitted successfully!");
        if (activeSection === 'bookings' || activeSection === 'book_equipment') fetchBookings();
      }
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message;
      if (errorMessage.toLowerCase().includes("already booked")) {
        if (window.confirm("This time slot is already booked for this equipment. Would you like to join the waitlist instead?")) {
          handleJoinWaitlist(e);
        }
      } else {
        alert("Failed to submit booking: " + errorMessage);
      }
    }
  };

  const handleCreateAccessRequest = async (e) => {
    e.preventDefault();

    if (accessRequestData.availableFrom && accessRequestData.availableTo) {
      const reqStart = new Date(accessRequestData.requestedStart);
      const reqEnd = new Date(accessRequestData.requestedEnd);
      const availStart = new Date(accessRequestData.availableFrom);
      const availEnd = new Date(accessRequestData.availableTo);

      if (reqStart < availStart || reqEnd > availEnd) {
        alert("This equipment is not available at that time.");
        return;
      }
    }

    try {
      await api.post('/v1/resource-sharing/requests', {
        listingId: accessRequestData.listingId,
        requesterId: user.id,
        justification: accessRequestData.justification,
        requestedStart: accessRequestData.requestedStart,
        requestedEnd: accessRequestData.requestedEnd
      });
      alert("Access request submitted successfully!");
      setShowAccessModal(false);
      if (activeSection === 'shared_resources') fetchMyRequests();
    } catch (err) {
      alert("Failed to submit access request: " + (err.response?.data?.message || err.message));
    }
  };

  const handleShareEquipment = async (e) => {
    e.preventDefault();
    try {
      await api.post('/v1/resource-sharing/listings', {
        equipmentId: shareData.equipmentId,
        termsAndConditions: shareData.termsAndConditions,
        availableFrom: shareData.availableFrom,
        availableTo: shareData.availableTo,
        isActive: true
      });
      alert("Equipment shared successfully!");
      setShowShareModal(false);
    } catch (err) {
      alert("Failed to share equipment: " + (err.response?.data?.message || err.message));
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

  const handleJoinWaitlist = async (e) => {
    e.preventDefault();
    try {
      await api.post('/v1/waitlists/join', {
        equipmentId: bookingData.equipmentId,
        userId: user.id,
        requestedStart: bookingData.startTime,
        requestedEnd: bookingData.endTime
      });
      alert("Successfully joined waitlist!");
      setShowBookingModal(false);
    } catch (err) {
      alert("Failed to join waitlist: " + (err.response?.data?.message || err.message));
    }
  };

  const handleCancelWaitlist = async (waitlistId) => {
    if (!window.confirm("Are you sure you want to cancel this waitlist request?")) return;
    try {
      await api.put(`/v1/waitlists/${waitlistId}/status?status=CANCELLED`);
      alert("Waitlist request cancelled successfully.");
      setShowWaitlistModal(false);
      fetchWaitlists();
    } catch (err) {
      alert("Failed to cancel waitlist: " + (err.response?.data?.message || err.message));
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
    <ErrorBoundary>
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
      {/* Live pending-approval badge */}
      {notifications.filter(n => n.referenceType === 'BOOKING_APPROVAL_REQUEST').length > 0 && (
        <span className="ml-auto min-w-[20px] h-5 flex items-center justify-center bg-red-500 text-white text-[10px] font-bold rounded-full px-1">
          {notifications.filter(n => n.referenceType === 'BOOKING_APPROVAL_REQUEST').length}
        </span>
      )}
    </button>

    <button onClick={() => setActiveSection('profile')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'profile' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
      <User size={20} />
      <span>Profiles</span>
    </button>
    <button onClick={() => setActiveSection('shared_resources')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'shared_resources' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
      <Network size={20} />
      <span>Shared Resources</span>
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
        <button onClick={() => setActiveSection('maintenance')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'maintenance' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
          <Wrench size={20} />
          <span>Maintenance</span>
        </button>
        <button onClick={() => setActiveSection('billing')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'billing' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
          <FileText size={20} />
          <span>Billing & Invoices</span>
        </button>
        {(isSystemAdmin || hasRole('INSTITUTION_ADMIN')) && (
          <>
            <button onClick={() => setActiveSection('analytics')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'analytics' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
              <Activity size={20} />
              <span>Analytics</span>
            </button>
          </>
        )}
        {(isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && (
          <button onClick={() => setActiveSection('utilization')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'utilization' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
            <Activity size={20} />
            <span>Utilization Analytics</span>
          </button>
        )}
        {(isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && (
          <button onClick={() => setActiveSection('reports')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'reports' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
            <FileText size={20} />
            <span>OEE Reports</span>
          </button>
        )}
      </>
    )}


    {(isSystemAdmin || hasRole('INSTITUTION_ADMIN')) && (
      <button onClick={() => setActiveSection('users')} className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium text-sm ${activeSection === 'users' ? 'bg-white shadow-sm text-brand-orange' : 'hover:bg-black/5 hover:text-gray-900'}`}>
        <User size={20} />
        <span>Users</span>
      </button>
    )}
  </div>
      </aside >

  {/* Main Content Area */ }
  < div className = "flex-1 flex flex-col h-screen overflow-hidden bg-gray-50/50" >
    {/* Top Navigation */ }
    < header className = "bg-white border-b border-gray-200/60 px-6 py-4 h-20 flex items-center justify-between shrink-0 z-10" >
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
          <div className="flex items-center gap-5 relative">
            {/* â”€â”€ Notification Bell â”€â”€ */}
            <div className="relative" ref={notificationRef}>
              <button
                id="notification-bell-btn"
                onClick={() => setShowNotifications(prev => !prev)}
                className="p-2 hover:bg-gray-100 rounded-full relative transition-colors text-gray-500 focus:outline-none focus:ring-2 focus:ring-orange-300"
                aria-label="Toggle notifications"
                aria-expanded={showNotifications}
                aria-haspopup="true"
              >
                <Bell size={20} />
                {notifications.length > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] flex items-center justify-center bg-red-500 text-white text-[10px] font-bold rounded-full border-2 border-white px-0.5">
                    {notifications.length > 99 ? '99+' : notifications.length}
                  </span>
                )}
              </button>

              {/* â”€â”€ Notification Panel â”€â”€ */}
              {showNotifications && (
                <div
                  role="dialog"
                  aria-label="Notifications panel"
                  className="absolute right-0 mt-2 w-96 bg-white rounded-2xl shadow-2xl border border-gray-100 z-50 overflow-hidden"
                  style={{ animation: 'notifSlideIn 0.15s ease-out' }}
                >
                  {/* Header */}
                  <div className="px-4 py-3.5 border-b border-gray-100 flex justify-between items-center bg-gray-50/80">
                    <div className="flex items-center gap-2">
                      <Bell size={16} className="text-gray-500" />
                      <h3 className="font-bold text-gray-800 text-sm">Notifications</h3>
                      {notifications.length > 0 && (
                        <span className="bg-red-100 text-red-600 text-xs font-semibold px-2 py-0.5 rounded-full">
                          {notifications.length} new
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-3">
                      {notifications.length > 0 && (
                        <button
                          onClick={async () => {
                            try {
                              await api.patch(`/notifications/user/${user.id}/read-all`);
                              setNotifications([]);
                            } catch (err) {}
                          }}
                          className="text-xs text-orange-500 font-semibold hover:text-orange-600 transition-colors"
                        >
                          Mark all read
                        </button>
                      )}
                      <button
                        onClick={() => setShowNotifications(false)}
                        className="p-1 hover:bg-gray-200 rounded-full transition-colors text-gray-400 hover:text-gray-600"
                        aria-label="Close notifications"
                      >
                        <X size={14} />
                      </button>
                    </div>
                  </div>

                  {/* Body */}
                  <div className="max-h-[420px] overflow-y-auto divide-y divide-gray-50">
                    {notifications.length === 0 ? (
                      <div className="py-10 flex flex-col items-center gap-3 text-center px-6">
                        <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center">
                          <Bell size={22} className="text-gray-300" />
                        </div>
                        <p className="text-sm font-medium text-gray-500">You're all caught up!</p>
                        <p className="text-xs text-gray-400">No new notifications at the moment.</p>
                      </div>
                    ) : (
                      notifications.map(n => {
                        // Type-based icon + color
                        const typeConfig = {
                          BOOKING:                { icon: <Calendar size={14}/>, bg: 'bg-blue-100',   text: 'text-blue-600',   label: 'Booking'        },
                          BOOKING_APPROVAL_REQUEST:{ icon: <Clock size={14}/>,    bg: 'bg-amber-100',  text: 'text-amber-600',  label: 'Approval'       },
                          EQUIPMENT:              { icon: <Server size={14}/>,   bg: 'bg-green-100',  text: 'text-green-600',  label: 'Equipment'      },
                          MAINTENANCE:            { icon: <Wrench size={14}/>,   bg: 'bg-orange-100', text: 'text-orange-600', label: 'Maintenance'    },
                          INVOICE:                { icon: <FileText size={14}/>, bg: 'bg-purple-100', text: 'text-purple-600', label: 'Invoice'        },
                          SHARING_REQUEST:        { icon: <Network size={14}/>,  bg: 'bg-teal-100',   text: 'text-teal-600',   label: 'Sharing'        },
                          ACCESS_REQUEST:         { icon: <Key size={14}/>,      bg: 'bg-rose-100',   text: 'text-rose-600',   label: 'Access'         },
                          WAITLIST:               { icon: <Clock size={14}/>,    bg: 'bg-slate-100',  text: 'text-slate-600',  label: 'Waitlist'       },
                          // New types
                          INSTITUTION:            { icon: <Building size={14}/>, bg: 'bg-indigo-100', text: 'text-indigo-600', label: 'Institution'    },
                          DEPARTMENT:             { icon: <Network size={14}/>,  bg: 'bg-violet-100', text: 'text-violet-600', label: 'Department'     },
                          CATEGORY:               { icon: <Tags size={14}/>,     bg: 'bg-lime-100',   text: 'text-lime-600',   label: 'Category'       },
                          CALIBRATION:            { icon: <Activity size={14}/>, bg: 'bg-emerald-100',text: 'text-emerald-600',label: 'Calibration'    },
                          REPORT:                 { icon: <FileText size={14}/>, bg: 'bg-sky-100',    text: 'text-sky-600',    label: 'OEE Report'     },
                          USER_MANAGEMENT:        { icon: <User size={14}/>,     bg: 'bg-pink-100',   text: 'text-pink-600',   label: 'User'           },
                          PROFILE:                { icon: <User size={14}/>,     bg: 'bg-gray-100',   text: 'text-gray-600',   label: 'Profile'        },
                        };
                        const cfg = typeConfig[n.referenceType] || { icon: <Bell size={14}/>, bg: 'bg-gray-100', text: 'text-gray-600', label: n.referenceType };

                        const timeAgo = (() => {
                          if (!n.createdAt) return '';
                          const diff = Date.now() - new Date(n.createdAt).getTime();
                          const mins = Math.floor(diff / 60000);
                          if (mins < 1) return 'Just now';
                          if (mins < 60) return `${mins}m ago`;
                          const hrs = Math.floor(mins / 60);
                          if (hrs < 24) return `${hrs}h ago`;
                          return `${Math.floor(hrs / 24)}d ago`;
                        })();

                        return (
                          <div
                            key={n.id}
                            className={`flex gap-3 px-4 py-3.5 cursor-pointer transition-colors group ${n.isRead ? 'hover:bg-gray-50' : 'bg-orange-50/40 hover:bg-orange-50/70'}`}
                            onClick={async () => {
                              try {
                                if (!n.isRead) {
                                  await api.patch(`/notifications/${n.id}/read`);
                                  setNotifications(prev => prev.filter(noti => noti.id !== n.id));
                                }
                                setSelectedNotification(n);
                                setShowNotifications(false);
                              } catch (err) {}
                            }}
                          >
                            {/* Icon Badge */}
                            <div className={`shrink-0 w-8 h-8 rounded-full flex items-center justify-center mt-0.5 ${cfg.bg} ${cfg.text}`}>
                              {cfg.icon}
                            </div>

                            {/* Content */}
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between gap-2 mb-0.5">
                                <span className={`text-[10px] font-bold uppercase tracking-wide ${cfg.text}`}>{cfg.label}</span>
                                <span className="text-[10px] text-gray-400 shrink-0">{timeAgo}</span>
                              </div>
                              <p className="text-xs text-gray-700 line-clamp-2 leading-relaxed">
                                {n.message || n.content}
                              </p>
                            </div>

                            {/* Dismiss button */}
                            <button
                              className="shrink-0 opacity-0 group-hover:opacity-100 p-1 hover:bg-gray-200 rounded-full transition-all text-gray-400 hover:text-gray-600"
                              onClick={async (e) => {
                                e.stopPropagation();
                                try {
                                  await api.patch(`/notifications/${n.id}/read`);
                                } catch (err) {}
                                setNotifications(prev => prev.filter(noti => noti.id !== n.id));
                              }}
                              aria-label="Dismiss notification"
                            >
                              <X size={12} />
                            </button>
                          </div>
                        );
                      })
                    )}
                  </div>

                  {/* Footer */}
                  {notifications.length > 0 && (
                    <div className="px-4 py-2.5 border-t border-gray-100 bg-gray-50/60 text-center">
                      <p className="text-[11px] text-gray-400">Click a notification to view details</p>
                    </div>
                  )}
                </div>
              )}
            </div>
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
        </header >

  {/* Scrollable Content */ }
  < main className = "flex-1 overflow-y-auto p-6 lg:p-10" >
    <div className="max-w-6xl mx-auto flex flex-col gap-8 pb-12">

      {/* Header Title based on Active Section */}
      {activeSection === 'profile' && (
        <div>
          <h1 className="text-3xl font-bold tracking-tight mb-2 text-gray-800">Profiles</h1>
          <p className="text-gray-500">Manage your personal profile and preferences.</p>
        </div>
      )}

      {activeSection !== 'profile' && activeSection !== 'maintenance' && activeSection !== 'billing' && activeSection !== 'analytics' && activeSection !== 'reports' && (
        <div>
          <h1 className="text-3xl font-bold tracking-tight mb-2 text-gray-800">Dashboard</h1>
          <p className="text-gray-500">Welcome to your Lab Resource Utilization Platform.</p>
        </div>
      )}
      {activeSection === 'maintenance' && (
        <div>
          <h1 className="text-3xl font-bold tracking-tight mb-2 text-gray-800">Maintenance & Calibration</h1>
          <p className="text-gray-500">Track equipment health, schedule repairs, and view calibration records.</p>
        </div>
      )}
      {activeSection === 'billing' && (
        <div>
          <h1 className="text-3xl font-bold tracking-tight mb-2 text-gray-800">Billing & Invoices</h1>
          <p className="text-gray-500">Manage departmental budgets and view invoices for equipment usage.</p>
        </div>
      )}
      {activeSection === 'analytics' && (
        <div>
          <h1 className="text-3xl font-bold tracking-tight mb-2 text-gray-800">Platform Analytics</h1>
          <p className="text-gray-500">Global insights into equipment utilization and booking statistics.</p>
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

      {/* Utilization Analytics Section */}
      {activeSection === 'utilization' && (
        <section className="flex flex-col gap-6 animate-fade-in">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold flex items-center gap-2 text-gray-800">
              <Activity size={28} className="text-brand-orange" />
              Utilization Analytics
            </h2>
            <div className="text-sm font-medium text-gray-500 bg-white px-4 py-2 rounded-xl border border-gray-100 shadow-sm">
              Last 7 Days
            </div>
          </div>

          {/* Top KPIs */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 flex flex-col relative overflow-hidden group">
              <div className="absolute -right-6 -top-6 bg-orange-50 w-24 h-24 rounded-full opacity-50 group-hover:scale-110 transition-transform duration-300"></div>
              <div className="text-gray-500 text-sm font-medium mb-1">Overall Utilization</div>
              <div className="text-4xl font-bold text-gray-800 flex items-baseline gap-2">
                {heatmapData.length > 0 ? (heatmapData.reduce((acc, curr) => acc + (curr?.avgUtilizationRate || 0), 0) / heatmapData.length).toFixed(1) : 0}%
                <span className="text-xs font-semibold text-green-500 bg-green-50 px-2 py-1 rounded-full">+2%</span>
              </div>
            </div>
            
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 flex flex-col relative overflow-hidden group">
              <div className="absolute -right-6 -top-6 bg-blue-50 w-24 h-24 rounded-full opacity-50 group-hover:scale-110 transition-transform duration-300"></div>
              <div className="text-gray-500 text-sm font-medium mb-1">Total Wasted (Idle) Hours</div>
              <div className="text-4xl font-bold text-gray-800">
                {heatmapData.length > 0 ? (heatmapData.reduce((acc, curr) => acc + (curr?.totalIdleMinutes || 0), 0) / 60).toFixed(1) : 0} <span className="text-lg">hrs</span>
              </div>
            </div>

            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 flex flex-col relative overflow-hidden group">
               <div className="absolute -right-6 -top-6 bg-green-50 w-24 h-24 rounded-full opacity-50 group-hover:scale-110 transition-transform duration-300"></div>
               <div className="text-gray-500 text-sm font-medium mb-1">Efficiency Score</div>
               <div className="text-4xl font-bold text-gray-800">
                 {heatmapData.length > 0 ? Math.min(100, Math.max(0, 100 - (heatmapData.reduce((acc, curr) => acc + (curr?.totalIdleMinutes || 0), 0) / 60 / 10))).toFixed(0) : 0}
                 <span className="text-lg text-gray-500">/100</span>
               </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Chart */}
            <div className="lg:col-span-2 bg-white rounded-3xl p-8 shadow-sm border border-gray-100 relative">
              <h3 className="font-semibold text-gray-800 mb-6 flex items-center gap-2">
                <Activity size={18} /> Booking vs Actual Usage
              </h3>
              {isFetchingHeatmap ? (
                <div className="flex justify-center items-center h-64"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
              ) : heatmapData.length > 0 ? (
                <div className="h-72 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart data={heatmapData.map(d => ({ date: d?.recordDate || '', booked: (d?.totalBookedMinutes || 0) / 60, used: (d?.totalUsedMinutes || 0) / 60, avgUtil: d?.avgUtilizationRate || 0 }))}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                      <XAxis dataKey="date" axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} dy={10} />
                      <YAxis yAxisId="left" axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} dx={-10} label={{ value: 'Hours', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }} />
                      <YAxis yAxisId="right" orientation="right" axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} dx={10} label={{ value: 'Util %', angle: 90, position: 'insideRight', fill: '#9CA3AF' }} />
                      <Tooltip
                        contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                        cursor={{ fill: '#F3F4F6' }}
                      />
                      <Legend verticalAlign="top" height={36}/>
                      <Bar yAxisId="left" dataKey="booked" name="Booked (Hrs)" fill="#E5E7EB" radius={[4, 4, 0, 0]} barSize={40} />
                      <Bar yAxisId="left" dataKey="used" name="Actual Used (Hrs)" fill="#f97316" radius={[4, 4, 0, 0]} barSize={40} />
                      <Line yAxisId="right" type="monotone" dataKey="avgUtil" name="Avg Utilization %" stroke="#3b82f6" strokeWidth={3} dot={{ r: 4, fill: '#3b82f6', strokeWidth: 2, stroke: '#fff' }} activeDot={{ r: 6 }} />
                    </ComposedChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <div className="flex justify-center items-center h-64 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                  <span className="text-gray-500 font-medium">No utilization data available for this period.</span>
                </div>
              )}
            </div>

            {/* Leaderboards */}
            <div className="flex flex-col gap-6">
              {/* Top Performers */}
              <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 flex flex-col h-full">
                <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2">
                  <Activity size={18} className="text-green-500" /> Highest Utilization
                </h3>
                {isFetchingPerformance ? (
                   <div className="flex justify-center items-center flex-1"><Loader2 size={24} className="animate-spin text-brand-orange" /></div>
                ) : (
                  <div className="flex flex-col gap-3">
                    {performanceData?.top?.map((item, idx) => (
                      <div key={item?.equipmentId || idx} className="flex justify-between items-center p-3 bg-gray-50 rounded-xl">
                         <div className="flex items-center gap-3">
                            <span className="text-gray-400 font-bold text-sm">#{idx + 1}</span>
                            <span className="font-semibold text-sm text-gray-700 truncate w-32">{item?.equipmentName || 'Unknown'}</span>
                         </div>
                         <div className="font-bold text-green-600">{Number(item?.avgUtilizationRate || 0).toFixed(1)}%</div>
                      </div>
                    ))}
                    {(!performanceData?.top || performanceData.top.length === 0) && (
                      <div className="text-sm text-gray-500 text-center py-4">No data available</div>
                    )}
                  </div>
                )}
              </div>

              {/* Bottom Performers */}
              <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 flex flex-col h-full">
                <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2">
                  <Activity size={18} className="text-red-500" /> Lowest Utilization
                </h3>
                {isFetchingPerformance ? (
                   <div className="flex justify-center items-center flex-1"><Loader2 size={24} className="animate-spin text-brand-orange" /></div>
                ) : (
                  <div className="flex flex-col gap-3">
                    {performanceData?.bottom?.map((item, idx) => (
                      <div key={item?.equipmentId || idx} className="flex justify-between items-center p-3 bg-gray-50 rounded-xl group relative overflow-hidden">
                         <div className="absolute inset-0 bg-red-50 opacity-0 group-hover:opacity-100 transition-opacity"></div>
                         <div className="flex items-center gap-3 relative z-10">
                            <span className="text-gray-400 font-bold text-sm">#{idx + 1}</span>
                            <span className="font-semibold text-sm text-gray-700 truncate w-32">{item?.equipmentName || 'Unknown'}</span>
                         </div>
                         <div className="flex flex-col items-end relative z-10">
                           <span className="font-bold text-red-500">{Number(item?.avgUtilizationRate || 0).toFixed(1)}%</span>
                           <span className="text-[10px] text-gray-500 font-medium">Idle: {Math.round(Number(item?.totalIdleMinutes || 0)/60)}h</span>
                         </div>
                      </div>
                    ))}
                    {(!performanceData?.bottom || performanceData.bottom.length === 0) && (
                      <div className="text-sm text-gray-500 text-center py-4">No data available</div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </section>
      )}

      {/* Shared Resources Section */}
      {activeSection === 'shared_resources' && (
        <div className="flex flex-col gap-8 animate-fade-in">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold flex items-center gap-2 text-gray-800">
              <Network size={28} className="text-brand-orange" />
              Inter-Institution Shared Resources
            </h2>
          </div>

          <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
            {/* Active Listings */}
            <div className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 relative">
              <h3 className="font-semibold text-gray-800 mb-6 flex items-center gap-2">
                <Server size={18} /> Available Shared Equipment
              </h3>
              {isFetchingShared ? (
                <div className="flex justify-center p-8"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
              ) : sharedResources.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {sharedResources.map(resource => (
                    <div
                      key={resource.id}
                      className="border border-gray-100 bg-gray-50/50 rounded-2xl p-5 hover:shadow-md transition-shadow cursor-pointer"
                      onClick={() => {
                        const equipment = equipmentList.find(eq => eq.id === resource.equipmentId);
                        if (equipment) {
                          setSelectedEquipmentDetails({
                            ...equipment,
                            sharedAvailableFrom: resource.availableFrom,
                            sharedAvailableTo: resource.availableTo
                          });
                        }
                      }}
                    >
                      <div className="font-bold text-gray-800 mb-1">{resource.equipmentName}</div>
                      <div className="text-xs text-gray-500 mb-4 font-medium tracking-wide uppercase">{resource.institutionName || 'Shared Equipment'}</div>
                      <div className="text-sm text-gray-600 mb-2 line-clamp-2">{resource.termsAndConditions}</div>
                      {resource.waitlistCount > 0 && (
                        <div className="mb-4">
                          <span className="bg-amber-100 text-amber-800 text-xs font-bold px-2.5 py-0.5 rounded-full">
                            {resource.waitlistCount} in queue
                          </span>
                        </div>
                      )}
                      <div className={`${resource.waitlistCount === 0 ? 'mb-4' : ''}`}></div>
                      {resource.institutionName === user?.institutionName ? (
                        <div className="w-full bg-gray-100 text-gray-500 font-semibold py-2 rounded-xl text-center text-sm">
                          Owned by Your Institution
                        </div>
                      ) : (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setAccessRequestData({ ...accessRequestData, listingId: resource.id, equipmentName: resource.equipmentName, availableFrom: resource.availableFrom, availableTo: resource.availableTo });
                            fetchOccupiedDates(resource.id);
                            setShowAccessModal(true);
                          }}
                          className="w-full bg-white border border-gray-200 hover:border-brand-orange hover:text-brand-orange text-gray-700 font-semibold py-2 rounded-xl transition-colors text-sm"
                        >
                          Request Access
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="flex justify-center items-center h-32 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                  <span className="text-gray-500 font-medium">No shared resources currently available.</span>
                </div>
              )}
            </div>

            {/* My Requests */}
            <div className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 relative">
              <h3 className="font-semibold text-gray-800 mb-6 flex items-center gap-2">
                <FileText size={18} /> My Access Requests
              </h3>
              {myRequests.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse whitespace-nowrap">
                    <thead>
                      <tr className="border-b border-gray-200 text-xs uppercase tracking-wider text-gray-500">
                        <th className="pb-3 pr-4 font-medium">Resource</th>
                        <th className="pb-3 px-4 font-medium">Status</th>
                        <th className="pb-3 pl-4 font-medium">Dates</th>
                      </tr>
                    </thead>
                    <tbody>
                      {myRequests.map((req, idx) => (
                        <tr key={idx} className="border-b border-gray-100 last:border-0 hover:bg-gray-50/50 transition-colors">
                          <td className="py-4 pr-4">
                            <div className="font-semibold text-gray-800">{req.equipmentName || "Shared Equipment"}</div>
                            <div className="text-xs text-gray-500 uppercase tracking-wider">{req.institutionName || ""}</div>
                          </td>
                          <td className="py-4 px-4">
                            <span className={`px-3 py-1 rounded-full text-xs font-bold tracking-wide ${req.status === 'APPROVED' ? 'bg-green-100 text-green-700' : req.status === 'REJECTED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'}`}>
                              {req.status}
                            </span>
                          </td>
                          <td className="py-4 pl-4 text-sm text-gray-600 font-medium">
                            {req.requestedStart ? new Date(req.requestedStart).toLocaleDateString() : 'N/A'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="flex justify-center items-center h-32 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                  <span className="text-gray-500 font-medium">You haven't made any requests yet.</span>
                </div>
              )}
            </div>
          </div>

          {/* Incoming Requests (For Institution Admins/Lab Managers) */}
          {(user?.roles?.includes('INSTITUTION_ADMIN') || user?.roles?.includes('DEPT_HEAD') || user?.roles?.includes('LAB_MANAGER')) && (
            <div className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 relative mt-8">
              <h3 className="font-semibold text-gray-800 mb-6 flex items-center gap-2">
                <FileText size={18} /> Incoming Access Requests
              </h3>
              {isFetchingIncoming ? (
                <div className="flex justify-center py-4"><Loader2 size={24} className="animate-spin text-brand-orange" /></div>
              ) : incomingRequests.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse whitespace-nowrap">
                    <thead>
                      <tr className="border-b border-gray-200 text-xs uppercase tracking-wider text-gray-500">
                        <th className="pb-3 pr-4 font-medium">Resource</th>
                        <th className="pb-3 px-4 font-medium">Requester</th>
                        <th className="pb-3 px-4 font-medium">Dates</th>
                        <th className="pb-3 px-4 font-medium">Justification</th>
                        <th className="pb-3 px-4 font-medium">Status</th>
                        <th className="pb-3 pl-4 font-medium">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {incomingRequests.map((req, idx) => (
                        <tr key={idx} className="border-b border-gray-100 last:border-0 hover:bg-gray-50/50 transition-colors cursor-pointer" onClick={() => setSelectedIncomingRequest(req)}>
                          <td className="py-4 pr-4">
                            <div className="font-semibold text-gray-800">{req.equipmentName || "Shared Equipment"}</div>
                          </td>
                          <td className="py-4 px-4 text-sm text-gray-800 font-medium">
                            {req.requesterName || 'N/A'}
                          </td>
                          <td className="py-4 px-4 text-sm text-gray-600 font-medium">
                            {req.requestedStart ? new Date(req.requestedStart).toLocaleDateString() : 'N/A'} - {req.requestedEnd ? new Date(req.requestedEnd).toLocaleDateString() : 'N/A'}
                          </td>
                          <td className="py-4 px-4 text-sm text-gray-600 truncate max-w-xs">
                            {req.justification}
                          </td>
                          <td className="py-4 px-4">
                            <span className={`px-3 py-1 rounded-full text-xs font-bold tracking-wide ${req.status === 'APPROVED' ? 'bg-green-100 text-green-700' : req.status === 'REJECTED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'}`}>
                              {req.status}
                            </span>
                          </td>
                          <td className="py-4 pl-4" onClick={(e) => e.stopPropagation()}>
                            <div className="flex gap-2">
                              {req.status !== 'APPROVED' && (
                                <button onClick={() => handleApproveRejectRequest(req.id, 'APPROVED')} className="px-3 py-1 bg-green-500 hover:bg-green-600 text-white rounded text-xs font-semibold transition-colors">Approve</button>
                              )}
                              {req.status !== 'REJECTED' && (
                                <button onClick={() => handleApproveRejectRequest(req.id, 'REJECTED')} className="px-3 py-1 bg-red-500 hover:bg-red-600 text-white rounded text-xs font-semibold transition-colors">Reject</button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="flex justify-center items-center h-24 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                  <span className="text-gray-500 font-medium text-sm">No incoming requests.</span>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Bookings Section (Dashboard) */}
      {activeSection === 'bookings' && (
        <div className="flex flex-col gap-6 animate-fade-in">
          {/* Quick Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Total Equipment Card */}
            <div
              className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex items-center gap-4 cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 group"
              onClick={() => {
                const isAuthorized = isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER');
                const relevantEquipment = isSystemAdmin ? equipmentList : equipmentList.filter(e => e.institutionId === user?.institutionId);
                setKpiModalTitle('Total Equipment');
                setKpiModalType('equipment');
                setKpiModalData(relevantEquipment);
                setKpiModalAuthorized(isAuthorized);
                setShowKpiModal(true);
              }}
              title="Click to view equipment details"
            >
              <div className="bg-blue-50 p-4 rounded-xl text-blue-600 group-hover:bg-blue-100 transition-colors"><Server size={24} /></div>
              <div className="flex-1">
                <div className="text-gray-500 text-sm font-medium">Total Equipment</div>
                <div className="text-2xl font-bold text-gray-800">{dashboardStats.totalEquipment}</div>
              </div>
              <ExternalLink size={14} className="text-gray-300 group-hover:text-gray-500 transition-colors" />
            </div>

            {/* Today's Bookings Card */}
            <div
              className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex items-center gap-4 cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 group"
              onClick={() => {
                const isAuthorized = isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER');
                const today = new Date().toDateString();
                const todayBookings = bookingsList.filter(b => new Date(b.startTime).toDateString() === today);
                setKpiModalTitle("Today's Bookings");
                setKpiModalType('bookings');
                setKpiModalData(todayBookings);
                setKpiModalAuthorized(isAuthorized);
                setShowKpiModal(true);
              }}
              title="Click to view today's bookings"
            >
              <div className="bg-green-50 p-4 rounded-xl text-green-600 group-hover:bg-green-100 transition-colors"><Calendar size={24} /></div>
              <div className="flex-1">
                <div className="text-gray-500 text-sm font-medium">Today's Bookings</div>
                <div className="text-2xl font-bold text-gray-800">{dashboardStats.todaysBookings}</div>
              </div>
              <ExternalLink size={14} className="text-gray-300 group-hover:text-gray-500 transition-colors" />
            </div>

            {/* Pending Approvals Card */}
            <div
              className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex items-center gap-4 cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 group"
              onClick={() => {
                const isAuthorized = isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER');
                const pendingBookings = bookingsList.filter(b => b.status === 'PENDING');
                setKpiModalTitle('Pending Approvals');
                setKpiModalType('pending');
                setKpiModalData(pendingBookings);
                setKpiModalAuthorized(isAuthorized);
                setShowKpiModal(true);
              }}
              title="Click to view pending approvals"
            >
              <div className="bg-amber-50 p-4 rounded-xl text-amber-600 group-hover:bg-amber-100 transition-colors"><Activity size={24} /></div>
              <div className="flex-1">
                <div className="text-gray-500 text-sm font-medium">Pending Approvals</div>
                <div className="text-2xl font-bold text-gray-800">{dashboardStats.pendingApprovals}</div>
              </div>
              <ExternalLink size={14} className="text-gray-300 group-hover:text-gray-500 transition-colors" />
            </div>

            {/* Under Maintenance Card */}
            <div
              className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex items-center gap-4 cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 group"
              onClick={() => {
                const isAuthorized = isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER');
                const relevantEquipment = isSystemAdmin ? equipmentList : equipmentList.filter(e => e.institutionId === user?.institutionId);
                const maintenanceEquipment = relevantEquipment.filter(e => e.status === 'UNDER_MAINTENANCE');
                setKpiModalTitle('Under Maintenance');
                setKpiModalType('maintenance');
                setKpiModalData(maintenanceEquipment);
                setKpiModalAuthorized(isAuthorized);
                setShowKpiModal(true);
              }}
              title="Click to view maintenance details"
            >
              <div className="bg-red-50 p-4 rounded-xl text-red-600 group-hover:bg-red-100 transition-colors"><Wrench size={24} /></div>
              <div className="flex-1">
                <div className="text-gray-500 text-sm font-medium">Under Maintenance</div>
                <div className="text-2xl font-bold text-gray-800">{dashboardStats.underMaintenance}</div>
              </div>
              <ExternalLink size={14} className="text-gray-300 group-hover:text-gray-500 transition-colors" />
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

          {/* Bookings Filter Bar */}
          <div className="bg-white rounded-2xl p-4 shadow-sm border border-gray-100 flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 flex-1 min-w-[200px]">
              <Search size={16} className="text-gray-400 shrink-0" />
              <input
                type="text"
                placeholder="Search by user or equipment..."
                value={bookingSearchQuery}
                onChange={(e) => { setBookingSearchQuery(e.target.value); setBookingCurrentPage(0); }}
                className="w-full text-sm outline-none text-gray-700 placeholder-gray-400"
              />
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              {['ALL', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'].map(status => (
                <button
                  key={status}
                  onClick={() => { setBookingFilterStatus(status); setBookingCurrentPage(0); }}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                    bookingFilterStatus === status
                      ? 'bg-brand-orange text-white shadow-sm'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  {status === 'ALL' ? 'All Statuses' : status.replace('_', ' ')}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6" id="recent-bookings">
            {/* Bookings Table */}
            <div className="lg:col-span-2 bg-white rounded-2xl p-6 shadow-sm border border-gray-100">
              <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2"><Calendar size={18} /> Recent Bookings</h3>
              {isFetchingBookings ? (
                <div className="flex justify-center p-8"><Loader2 size={32} className="animate-spin text-brand-orange" /></div>
              ) : (() => {
                const filtered = bookingsList
                  .filter(b => bookingFilterStatus === 'ALL' || b.status === bookingFilterStatus)
                  .filter(b => {
                    const q = bookingSearchQuery.toLowerCase();
                    return !q || (b.userName || '').toLowerCase().includes(q) || (b.equipmentName || '').toLowerCase().includes(q);
                  });
                const totalPages = Math.ceil(filtered.length / BOOKING_PAGE_SIZE);
                const paginated = filtered.slice(bookingCurrentPage * BOOKING_PAGE_SIZE, (bookingCurrentPage + 1) * BOOKING_PAGE_SIZE);
                return filtered.length > 0 ? (
                  <>
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
                          {paginated.map(b => (
                            <tr key={b.id} onClick={() => setSelectedBookingDetails(b)} className="cursor-pointer border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                              <td className="py-3 pr-4 font-medium text-gray-800 text-sm">{b.userName}</td>
                              <td className="py-3 px-4 text-gray-600 text-sm">{b.equipmentName}</td>
                              <td className="py-3 px-4 text-gray-500 text-xs">
                                {new Date(b.startTime).toLocaleDateString()} {new Date(b.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </td>
                              <td className="py-3 px-4">
                                <span className={`px-2 py-1 rounded text-[11px] font-bold tracking-wide ${b.status === 'CONFIRMED' ? 'bg-green-100 text-green-700' : b.status === 'PENDING' ? 'bg-amber-100 text-amber-700' : b.status === 'CANCELLED' ? 'bg-red-100 text-red-700' : b.status === 'COMPLETED' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-700'}`}>
                                  {b.status}
                                </span>
                              </td>
                              <td className="py-3 pl-4" onClick={(e) => e.stopPropagation()}>
                                {(isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && b.status === 'PENDING' ? (
                                  <div className="flex items-center gap-1.5">
                                    <button
                                      onClick={() => handleUpdateBookingStatus(b.id, 'CONFIRMED')}
                                      className="px-2 py-1 bg-green-500 hover:bg-green-600 text-white rounded text-xs font-semibold transition-colors flex items-center gap-1"
                                      title="Approve booking"
                                    >
                                      <CheckCircle size={12} /> Approve
                                    </button>
                                    <button
                                      onClick={() => { if(window.confirm('Reject this booking request?')) handleUpdateBookingStatus(b.id, 'CANCELLED'); }}
                                      className="px-2 py-1 bg-red-500 hover:bg-red-600 text-white rounded text-xs font-semibold transition-colors flex items-center gap-1"
                                      title="Reject booking"
                                    >
                                      <X size={12} /> Reject
                                    </button>
                                  </div>
                                ) : (isSystemAdmin || hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && ['CONFIRMED', 'IN_USE'].includes(b.status) ? (
                                  <select
                                    className="border border-gray-200 rounded px-2 py-1 text-xs bg-white text-gray-700 focus:outline-none focus:border-brand-orange"
                                    value={b.status}
                                    onChange={(e) => handleUpdateBookingStatus(b.id, e.target.value)}
                                    onClick={(e) => e.stopPropagation()}
                                  >
                                    <option value="CONFIRMED">Confirmed</option>
                                    <option value="IN_USE">In Use</option>
                                    <option value="COMPLETED">Completed</option>
                                    <option value="NO_SHOW">No Show</option>
                                  </select>
                                ) : b.userId === user?.id && !['CANCELLED', 'COMPLETED', 'NO_SHOW'].includes(b.status) ? (
                                  <button
                                    onClick={(e) => { e.stopPropagation(); if (window.confirm('Cancel this booking?')) handleUpdateBookingStatus(b.id, 'CANCELLED'); }}
                                    className="text-red-600 hover:text-red-800 text-xs font-medium bg-red-50 hover:bg-red-100 px-2 py-1 rounded transition-colors border border-red-200"
                                  >
                                    Cancel
                                  </button>
                                ) : (
                                  <span className="text-gray-400 text-xs">—</span>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    {/* Pagination */}
                    {totalPages > 1 && (
                      <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-100">
                        <span className="text-xs text-gray-500">
                          Showing {bookingCurrentPage * BOOKING_PAGE_SIZE + 1}–{Math.min((bookingCurrentPage + 1) * BOOKING_PAGE_SIZE, filtered.length)} of {filtered.length}
                        </span>
                        <div className="flex items-center gap-1">
                          <button
                            disabled={bookingCurrentPage === 0}
                            onClick={() => setBookingCurrentPage(p => p - 1)}
                            className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                          >← Prev</button>
                          {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                            const page = totalPages <= 5 ? i : Math.max(0, Math.min(bookingCurrentPage - 2, totalPages - 5)) + i;
                            return (
                              <button
                                key={page}
                                onClick={() => setBookingCurrentPage(page)}
                                className={`w-8 h-8 text-xs font-semibold rounded-lg transition-colors ${
                                  bookingCurrentPage === page ? 'bg-brand-orange text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                }`}
                              >{page + 1}</button>
                            );
                          })}
                          <button
                            disabled={bookingCurrentPage >= totalPages - 1}
                            onClick={() => setBookingCurrentPage(p => p + 1)}
                            className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                          >Next →</button>
                        </div>
                      </div>
                    )}
                  </>
                ) : (
                  <div className="text-center p-8 text-gray-500 text-sm bg-gray-50 rounded-xl border border-dashed border-gray-200">
                    {bookingSearchQuery || bookingFilterStatus !== 'ALL' ? 'No bookings match your filters.' : 'No recent bookings found.'}
                  </div>
                );
              })()}
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
                <button onClick={() => setBookingTab('waitlists')} className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${bookingTab === 'waitlists' ? 'bg-white shadow-sm text-brand-orange' : 'text-gray-500 hover:text-gray-700'}`}>
                  My Waitlists
                </button>
              </div>
            )}
            {activeSection === 'equipment' && (!isSystemAdmin && (hasRole('LAB_MANAGER') || hasRole('DEPT_HEAD'))) && (
              <div className="flex bg-gray-100 rounded-lg p-1">
                <button onClick={() => setEquipmentManagementTab('add')} className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${equipmentManagementTab === 'add' ? 'bg-white shadow-sm text-brand-orange' : 'text-gray-500 hover:text-gray-700'}`}>
                  Equipment Management
                </button>
                <button onClick={() => setEquipmentManagementTab('view')} className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${equipmentManagementTab === 'view' ? 'bg-white shadow-sm text-brand-orange' : 'text-gray-500 hover:text-gray-700'}`}>
                  View & Update Equipment
                </button>
                <button onClick={() => setEquipmentManagementTab('shared')} className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${equipmentManagementTab === 'shared' ? 'bg-white shadow-sm text-brand-orange' : 'text-gray-500 hover:text-gray-700'}`}>
                  Shared Equipment
                </button>
              </div>
            )}
          </div>

          {(!isSystemAdmin && (hasRole('LAB_MANAGER') || hasRole('DEPT_HEAD'))) && activeSection === 'equipment' && equipmentManagementTab === 'add' && (
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
                <input type="number" step="0.01" name="pricePerHour" placeholder="Price Per Hour (₹)" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
                <input type="file" name="imageFile" accept="image/*" className="rounded-full border border-gray-300 px-5 py-2 outline-none w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-medium file:bg-purple-50 file:text-purple-700 hover:file:bg-purple-100" />
                <textarea name="description" placeholder="Description" className="rounded-2xl border border-gray-300 px-5 py-3 outline-none w-full md:col-span-2" rows="2"></textarea>
                <button type="submit" className="md:col-span-2 bg-purple-600 hover:bg-purple-700 text-white px-8 py-2.5 rounded-full font-medium transition-colors w-fit justify-self-start">Add Equipment</button>
              </form>
            </div>

          )}

          {((activeSection === 'book_equipment' && bookingTab === 'book' && !hasRole('INSTITUTION_ADMIN')) || (activeSection === 'equipment' && equipmentManagementTab === 'view') || (activeSection === 'equipment' && (isSystemAdmin || hasRole('INSTITUTION_ADMIN')))) && (
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
                            <tr key={eq.id} onClick={() => setSelectedEquipmentDetails(eq)} className="cursor-pointer border-b border-gray-100 hover:bg-gray-50/50 transition-colors">
                              <td className="py-3 px-4"><div className="font-medium text-gray-800">{eq.name}</div><div className="text-xs text-gray-500">SN: {eq.serialNumber} | Mfr: {eq.manufacturer} | ₹{eq.pricePerHour}/hr</div></td>
                              <td className="py-3 px-4 text-gray-600 text-sm">{eq.institutionName}<br /><span className="text-xs text-gray-400">{eq.departmentName}</span></td>
                              <td className="py-3 px-4 text-gray-600 text-sm">{eq.categoryName}</td>
                              <td className="py-3 px-4"><span className={`px-2 py-1 rounded text-xs font-medium ${eq.status === 'AVAILABLE' ? 'bg-green-100 text-green-700' : eq.status === 'BOOKED' ? 'bg-blue-100 text-blue-700' : 'bg-red-100 text-red-700'}`}>{eq.status}</span></td>
                              {!isSystemAdmin && !hasRole('INSTITUTION_ADMIN') && !isOtherInstitutes && activeSection !== 'book_equipment' && (
                                <td className="py-3 px-4" onClick={(e) => e.stopPropagation()}>
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
                                <td className="py-3 px-4 flex gap-2" onClick={(e) => e.stopPropagation()}>
                                  {(() => {
                                    const buttons = [];
                                    if (eq.status === 'AVAILABLE') {
                                      const isOwnInstitution = eq.institutionId === user?.institutionId;
                                      const isManagerRole = hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER');

                                      // Booking logic: Hide book button if manager viewing own equipment or if Inst Admin
                                      const showBook = !(hasRole('INSTITUTION_ADMIN') || (isManagerRole && isOwnInstitution));
                                      if (showBook) {
                                        buttons.push(
                                          <button
                                            key="book"
                                            onClick={() => {
                                              const now = new Date();
                                              const tzOffset = now.getTimezoneOffset() * 60000;
                                              const localNow = new Date(now.getTime() - tzOffset);
                                              const localLater = new Date(now.getTime() - tzOffset + 60 * 60 * 1000);
                                              setBookingData({
                                                ...bookingData,
                                                equipmentId: eq.id,
                                                equipmentName: eq.name,
                                                imageBase64: eq.imageBase64,
                                                startTime: localNow.toISOString().slice(0, 16),
                                                endTime: localLater.toISOString().slice(0, 16)
                                              });
                                              setShowBookingModal(true);
                                            }}
                                            className="bg-brand-orange hover:bg-orange-600 text-white px-3 py-1.5 rounded-full text-xs font-medium transition-colors"
                                          >
                                            Book
                                          </button>
                                        );
                                      }
                                    }

                                    // Sharing logic: Only owners (admins/managers) can share their own equipment from the Equipment tab
                                    if (activeSection === 'equipment' && eq.institutionId === user?.institutionId && (hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER'))) {
                                      buttons.push(
                                        <button
                                          key="edit"
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            setEditingEquipment(eq);
                                            setShowEditEquipmentModal(true);
                                          }}
                                          className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-3 py-1.5 rounded-full text-xs font-medium transition-colors"
                                        >
                                          Edit
                                        </button>
                                      );
                                      buttons.push(
                                        <button
                                          key="share"
                                          onClick={(e) => { e.stopPropagation(); setShareData({ equipmentId: eq.id, equipmentName: eq.name, termsAndConditions: '', availableFrom: '', availableTo: '' }); setShowShareModal(true); }}
                                          className="bg-blue-100 hover:bg-blue-200 text-blue-700 px-3 py-1.5 rounded-full text-xs font-medium transition-colors"
                                        >
                                          Share
                                        </button>
                                      );
                                    }

                                    return buttons;
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

                  const paginatedMyEquipment = activeSection === 'equipment'
                    ? myEquipment.slice((myEquipmentPage - 1) * itemsPerPage, myEquipmentPage * itemsPerPage)
                    : myEquipment;
                  const totalMyPages = Math.ceil(myEquipment.length / itemsPerPage);

                  return (
                    <div className="flex flex-col gap-8">
                      {activeSection === 'equipment' && (
                        <div>
                          <h4 className="font-semibold text-lg text-gray-700 mb-3">My Institute Equipment</h4>
                          {renderTable(paginatedMyEquipment, false)}
                          {totalMyPages > 1 && (
                            <div className="flex justify-center items-center mt-4 gap-2">
                              <button onClick={() => setMyEquipmentPage(Math.max(1, myEquipmentPage - 1))} disabled={myEquipmentPage === 1} className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50">Prev</button>
                              <span className="text-sm text-gray-600">Page {myEquipmentPage} of {totalMyPages}</span>
                              <button onClick={() => setMyEquipmentPage(Math.min(totalMyPages, myEquipmentPage + 1))} disabled={myEquipmentPage === totalMyPages} className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50">Next</button>
                            </div>
                          )}
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

                // Apply pagination to all equipment view if Inst Admin or global search
                const paginatedList = equipmentList.slice((myEquipmentPage - 1) * itemsPerPage, myEquipmentPage * itemsPerPage);
                const totalListPages = Math.ceil(equipmentList.length / itemsPerPage);

                return (
                  <div className="flex flex-col gap-8">
                    {equipmentManagementTab !== 'shared' && (
                      <div>
                        {renderTable(paginatedList, false)}
                        {totalListPages > 1 && (
                          <div className="flex justify-center items-center mt-4 gap-2">
                            <button onClick={() => setMyEquipmentPage(Math.max(1, myEquipmentPage - 1))} disabled={myEquipmentPage === 1} className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50">Prev</button>
                            <span className="text-sm text-gray-600">Page {myEquipmentPage} of {totalListPages}</span>
                            <button onClick={() => setMyEquipmentPage(Math.min(totalListPages, myEquipmentPage + 1))} disabled={myEquipmentPage === totalListPages} className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50">Next</button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })()}
            </div>
          )}

          {activeSection === 'equipment' && equipmentManagementTab === 'shared' && (
            <div className="bg-gray-50 p-6 rounded-xl border border-gray-200">
              <h3 className="font-medium text-lg mb-4">Shared Equipment</h3>
              {sharedResources.length > 0 ? (
                <div className="overflow-x-auto border border-gray-200 rounded-xl">
                  <table className="w-full text-left border-collapse bg-white">
                    <thead>
                      <tr className="border-b border-gray-200 bg-gray-50">
                        <th className="py-3 px-4 text-sm font-medium text-gray-500">Equipment</th>
                        <th className="py-3 px-4 text-sm font-medium text-gray-500">Owner Institute</th>
                        <th className="py-3 px-4 text-sm font-medium text-gray-500">Availability</th>
                        <th className="py-3 px-4 text-sm font-medium text-gray-500">Terms</th>
                        <th className="py-3 px-4 text-sm font-medium text-gray-500">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sharedResources.map(listing => (
                        <tr
                          key={listing.id}
                          className={`border-b border-gray-100 transition-colors cursor-pointer hover:bg-gray-50`}
                          onClick={() => {
                            const equipment = equipmentList.find(eq => eq.id === listing.equipmentId);
                            if (equipment) {
                              setSelectedEquipmentDetails({
                                ...equipment,
                                sharedAvailableFrom: listing.availableFrom,
                                sharedAvailableTo: listing.availableTo
                              });
                            } else {
                              api.get(`/equipment/${listing.equipmentId}`).then(res => {
                                setSelectedEquipmentDetails({
                                  ...res.data,
                                  sharedAvailableFrom: listing.availableFrom,
                                  sharedAvailableTo: listing.availableTo
                                });
                              }).catch(err => {
                                console.error("Failed to fetch equipment details", err);
                                setSelectedEquipmentDetails({
                                  name: listing.equipmentName,
                                  institutionName: listing.institutionName,
                                  sharedAvailableFrom: listing.availableFrom,
                                  sharedAvailableTo: listing.availableTo,
                                  status: 'UNKNOWN'
                                });
                              });
                            }
                          }}
                        >
                          <td className="py-3 px-4 font-medium text-gray-800">{listing.equipmentName}</td>
                          <td className="py-3 px-4 text-gray-600 text-sm">{listing.institutionName || 'N/A'}</td>
                          <td className="py-3 px-4 text-gray-600 text-sm">
                            {listing.availableFrom ? new Date(listing.availableFrom).toLocaleDateString() : 'N/A'} - {listing.availableTo ? new Date(listing.availableTo).toLocaleDateString() : 'N/A'}
                          </td>
                          <td className="py-3 px-4 text-gray-600 text-sm truncate max-w-xs">{listing.termsAndConditions || 'None'}</td>
                          <td className="py-3 px-4">
                            {listing.institutionId !== user?.institutionId ? (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setAccessRequestData({ ...accessRequestData, listingId: listing.id, equipmentName: listing.equipmentName, availableFrom: listing.availableFrom, availableTo: listing.availableTo });
                                  fetchOccupiedDates(listing.id);
                                  setShowAccessModal(true);
                                }}
                                className="bg-brand-orange hover:bg-orange-600 text-white px-3 py-1.5 rounded-full text-xs font-medium transition-colors"
                              >
                                Request Access
                              </button>
                            ) : (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setShareData({
                                    equipmentId: listing.equipmentId,
                                    equipmentName: listing.equipmentName,
                                    availableFrom: listing.availableFrom,
                                    availableTo: listing.availableTo,
                                    termsAndConditions: listing.termsAndConditions
                                  });
                                  setShowShareModal(true);
                                }}
                                className="text-xs text-blue-600 hover:text-blue-800 font-medium bg-blue-50 hover:bg-blue-100 px-2 py-1 rounded transition-colors"
                              >
                                Edit Share
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="text-center p-8 bg-white rounded-xl border border-dashed border-gray-300 text-gray-500">No shared equipment found at this time.</div>
              )}
            </div>
          )}
        </section>
      )}

      {/* 6. Booking History View */}
      {(activeSection === 'book_equipment' && (bookingTab === 'history' || hasRole('INSTITUTION_ADMIN') || isSystemAdmin)) && (
        <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in flex flex-col gap-8">
          <div className="flex justify-between items-center flex-wrap gap-4">
            <h2 className="text-xl font-medium flex items-center gap-2"><ShoppingCart size={24} className="text-brand-orange" /> {isSystemAdmin ? 'Global Bookings Overview' : 'Booking History'}</h2>
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
              </div>
              {isSystemAdmin && (
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-500 font-medium">Filter by Institute:</span>
                <select
                  className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm bg-white text-gray-700 focus:outline-none focus:border-brand-orange"
                  value={bookingHistoryInstitutionFilter}
                  onChange={(e) => {
                    setBookingHistoryInstitutionFilter(e.target.value);
                    setBookingHistoryPage(1);
                  }}
                >
                  <option value="">All Institutes</option>
                  {institutions.map(inst => (
                    <option key={inst.id} value={inst.id}>{inst.name}</option>
                  ))}
                </select>
                </div>
              )}
            </div>
          </div>
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
                  let historyBookings = isSystemAdmin ? bookingsList : bookingsList.filter(b => b.userId === user?.id || b.userInstitutionId === user?.institutionId);
                  
                  if (isSystemAdmin && bookingHistoryInstitutionFilter) {
                    historyBookings = historyBookings.filter(b => 
                      b.userInstitutionId?.toString() === bookingHistoryInstitutionFilter || 
                      b.equipmentInstitutionId?.toString() === bookingHistoryInstitutionFilter
                    );
                  }
                  if (bookingHistorySearchFilter) {
                    const searchLower = bookingHistorySearchFilter.toLowerCase();
                    historyBookings = historyBookings.filter(b => 
                      b.equipmentName?.toLowerCase().includes(searchLower) ||
                      b.userName?.toLowerCase().includes(searchLower) ||
                      b.userInstitutionName?.toLowerCase().includes(searchLower) ||
                      b.equipmentInstitutionName?.toLowerCase().includes(searchLower) ||
                      b.status?.toLowerCase().includes(searchLower)
                    );
                  }
                  if (bookingHistoryStatusFilter) {
                    historyBookings = historyBookings.filter(b => b.status === bookingHistoryStatusFilter);
                  }
                  
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
                        <span className={`px-2 py-1 rounded text-[11px] font-bold tracking-wide ${b.status === 'CONFIRMED' ? 'bg-green-100 text-green-700' : b.status === 'PENDING' ? 'bg-amber-100 text-amber-700' : b.status === 'PENDING_PAYMENT' ? 'bg-orange-100 text-orange-700' : b.status === 'CANCELLED' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'}`}>
                          {b.status.replace('_', ' ')}
                        </span>
                      </td>
                      <td className="py-3 pl-4" onClick={(e) => e.stopPropagation()}>
                        {b.status === 'PENDING_PAYMENT' && b.userId !== user?.id ? (
                          <span className="text-gray-400 text-xs font-medium">Awaiting Payment (Invoice {b.invoiceId?.substring(0, 8)})</span>
                        ) : b.status === 'PENDING_PAYMENT' && b.userId === user?.id ? (
                          <button onClick={(e) => { e.stopPropagation(); setSelectedInvoiceToPay({ id: b.invoiceId, totalAmount: b.totalAmount }); setIsPaymentModalOpen(true); }} className="bg-brand-orange text-white px-2 py-1 rounded text-xs font-medium">Pay Now</button>
                        ) : (!isSystemAdmin && (hasRole('DEPT_HEAD') || hasRole('LAB_MANAGER')) && b.equipmentInstitutionId === user?.institutionId) ? (
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
            let historyBookings = isSystemAdmin ? bookingsList : bookingsList.filter(b => b.userId === user?.id || b.userInstitutionId === user?.institutionId);
            if (isSystemAdmin && bookingHistoryInstitutionFilter) {
              historyBookings = historyBookings.filter(b => 
                b.userInstitutionId?.toString() === bookingHistoryInstitutionFilter || 
                b.equipmentInstitutionId?.toString() === bookingHistoryInstitutionFilter
              );
            }
            if (bookingHistorySearchFilter) {
              const searchLower = bookingHistorySearchFilter.toLowerCase();
              historyBookings = historyBookings.filter(b => 
                b.equipmentName?.toLowerCase().includes(searchLower) ||
                b.userName?.toLowerCase().includes(searchLower) ||
                b.userInstitutionName?.toLowerCase().includes(searchLower) ||
                b.equipmentInstitutionName?.toLowerCase().includes(searchLower) ||
                b.status?.toLowerCase().includes(searchLower)
              );
            }
            if (bookingHistoryStatusFilter) {
              historyBookings = historyBookings.filter(b => b.status === bookingHistoryStatusFilter);
            }
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

      {/* 7. Waitlists View */}
      {(activeSection === 'book_equipment' && bookingTab === 'waitlists') && (
        <section className="bg-white rounded-2xl p-8 shadow-sm border border-gray-200 animate-fade-in flex flex-col gap-8">
          <h2 className="text-xl font-medium flex items-center gap-2"><Clock size={24} className="text-brand-orange" /> My Waitlists</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse whitespace-nowrap">
              <thead>
                <tr className="border-b border-gray-200 text-xs uppercase tracking-wider text-gray-500">
                  <th className="pb-3 pr-4 font-medium">Equipment</th>
                  <th className="pb-3 px-4 font-medium">Requested Time Slot</th>
                  <th className="pb-3 px-4 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {isFetchingWaitlists ? (
                  <tr><td colSpan="3" className="py-8 text-center text-gray-500"><Loader2 size={24} className="animate-spin mx-auto text-brand-orange" /></td></tr>
                ) : waitlistsList.length === 0 ? (
                  <tr><td colSpan="3" className="py-8 text-center text-gray-500">You haven't joined any waitlists.</td></tr>
                ) : waitlistsList.map(w => (
                  <tr key={w.id} onClick={() => { setSelectedWaitlistDetails(w); setShowWaitlistModal(true); }} className="cursor-pointer border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                    <td className="py-3 pr-4 font-medium text-gray-800 text-sm">{w.equipmentName}</td>
                    <td className="py-3 px-4 text-gray-500 text-xs">
                      {new Date(w.requestedStart).toLocaleDateString()} {new Date(w.requestedStart).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      {' - '}
                      {new Date(w.requestedEnd).toLocaleDateString()} {new Date(w.requestedEnd).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </td>
                    <td className="py-3 px-4">
                      <span className={`px-2 py-1 rounded text-[11px] font-bold tracking-wide ${w.status === 'ACTIVE' ? 'bg-amber-100 text-amber-700' : w.status === 'FULFILLED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>
                        {w.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {activeSection === 'maintenance' && (
        <section className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 animate-fade-in relative">
          <h2 className="text-xl font-bold flex items-center gap-2 mb-6 text-gray-800">
            <Wrench size={24} className="text-brand-orange" />
            Maintenance Tasks
          </h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-gray-500 uppercase">
                  <th className="pb-3 font-semibold">Equipment</th>
                  <th className="pb-3 font-semibold">Task Type</th>
                  <th className="pb-3 font-semibold">Status</th>
                  <th className="pb-3 font-semibold">Scheduled Date</th>
                  <th className="pb-3 font-semibold">Completed Date</th>
                </tr>
              </thead>
              <tbody>
                {(Array.isArray(maintenanceTasks) ? maintenanceTasks : []).map(task => (
                  <tr key={task.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                    <td className="py-4 font-medium text-gray-800">{task.equipmentName}</td>
                    <td className="py-4 text-gray-600">{task.taskType}</td>
                    <td className="py-4">
                      <span className={`px-3 py-1 rounded-full text-xs font-bold ${task.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                        task.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-700' :
                          'bg-yellow-100 text-yellow-700'
                        }`}>
                        {task.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="py-4 text-gray-600">{new Date(task.scheduledDate).toLocaleDateString()}</td>
                    <td className="py-4 text-gray-600">{task.completionDate ? new Date(task.completionDate).toLocaleDateString() : '-'}</td>
                  </tr>
                ))}
                {maintenanceTasks.length === 0 && (
                  <tr>
                    <td colSpan="5" className="py-8 text-center text-gray-500 bg-gray-50 rounded-xl">No maintenance tasks found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <h2 className="text-xl font-bold flex items-center gap-2 mt-12 mb-6 text-gray-800">
            <CheckCircle size={24} className="text-brand-orange" />
            Calibration Records
          </h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-gray-500 uppercase">
                  <th className="pb-3 font-semibold">Equipment</th>
                  <th className="pb-3 font-semibold">Calibration Date</th>
                  <th className="pb-3 font-semibold">Next Due Date</th>
                  <th className="pb-3 font-semibold">Performed By</th>
                  <th className="pb-3 font-semibold">Result</th>
                </tr>
              </thead>
              <tbody>
                {(Array.isArray(calibrationRecords) ? calibrationRecords : []).map(record => (
                  <tr key={record.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                    <td className="py-4 font-medium text-gray-800">{record.equipmentName}</td>
                    <td className="py-4 text-gray-600">{new Date(record.calibrationDate).toLocaleDateString()}</td>
                    <td className="py-4 text-gray-600">{new Date(record.nextDueDate).toLocaleDateString()}</td>
                    <td className="py-4 text-gray-600">{record.performedBy}</td>
                    <td className="py-4">
                      <span className={`px-3 py-1 rounded-full text-xs font-bold ${record.result === 'VALID' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                        }`}>
                        {record.result}
                      </span>
                    </td>
                  </tr>
                ))}
                {calibrationRecords.length === 0 && (
                  <tr>
                    <td colSpan="5" className="py-8 text-center text-gray-500 bg-gray-50 rounded-xl">No calibration records found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* — Module 1 Enhancement: Work Order Create Form ———————————————— */}
          {canManage && (
            <div className="mt-10">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold flex items-center gap-2 text-gray-800">
                  <Settings size={22} className="text-indigo-500" /> Work Order Management
                </h2>
                <button
                  onClick={() => setShowWorkOrderForm(v => !v)}
                  className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-xl text-sm font-semibold transition-colors shadow-sm"
                >
                  <Plus size={16} /> {showWorkOrderForm ? 'Cancel' : 'New Work Order'}
                </button>
              </div>

              {showWorkOrderForm && (
                <div className="bg-indigo-50 border border-indigo-100 rounded-2xl p-6 mb-6 animate-fade-in">
                  <h3 className="font-semibold text-indigo-800 mb-4">Create Work Order</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <select value={workOrderForm.equipmentId} onChange={e => setWorkOrderForm(f => ({...f, equipmentId: e.target.value}))}
                      className="border border-indigo-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-indigo-400">
                      <option value="">Select Equipment *</option>
                      {equipmentList.map(eq => <option key={eq.id} value={eq.id}>{eq.name}</option>)}
                    </select>
                    <select value={workOrderForm.taskType} onChange={e => setWorkOrderForm(f => ({...f, taskType: e.target.value}))}
                      className="border border-indigo-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-indigo-400">
                      <option value="PREVENTIVE">Preventive</option>
                      <option value="INSPECTION">Inspection</option>
                      <option value="REPAIR">Repair</option>
                    </select>
                    <select value={workOrderForm.priority} onChange={e => setWorkOrderForm(f => ({...f, priority: e.target.value}))}
                      className="border border-indigo-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-indigo-400">
                      <option value="LOW">Low</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HIGH">High</option>
                      <option value="CRITICAL">Critical</option>
                    </select>
                    <input type={workOrderForm.scheduledDate ? "date" : "text"} onFocus={e => e.target.type = 'date'} onBlur={e => e.target.type = workOrderForm.scheduledDate ? 'date' : 'text'}
                      value={workOrderForm.scheduledDate} onChange={e => setWorkOrderForm(f => ({...f, scheduledDate: e.target.value}))}
                      className="border border-indigo-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-indigo-400"
                      placeholder="Scheduled Date *" />
                    <select value={workOrderForm.technicianId} onChange={e => setWorkOrderForm(f => ({...f, technicianId: e.target.value}))}
                      className="border border-indigo-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-indigo-400">
                      <option value="">Assign Technician (optional)</option>
                      {usersList.map(u => <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>)}
                    </select>
                    <textarea value={workOrderForm.description} onChange={e => setWorkOrderForm(f => ({...f, description: e.target.value}))}
                      placeholder="Description / Notes"
                      className="border border-indigo-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-indigo-400 md:col-span-2" rows={2} />
                  </div>
                  <div className="flex justify-end mt-4">
                    <button onClick={handleSubmitWorkOrder} disabled={isSubmittingWorkOrder}
                      className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2.5 rounded-xl font-semibold text-sm disabled:opacity-60 flex items-center gap-2">
                      {isSubmittingWorkOrder ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle size={14} />}
                      Submit Work Order
                    </button>
                  </div>
                </div>
              )}

              {/* Work Order table with status filter + transition actions */}
              <div className="flex gap-2 mb-4 flex-wrap">
                {['ALL','CREATED','ASSIGNED','IN_PROGRESS','COMPLETED','VERIFIED'].map(s => (
                  <button key={s} onClick={() => setWorkOrderStatusFilter(s)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${workOrderStatusFilter === s ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
                    {s}
                  </button>
                ))}
              </div>
              <div className="overflow-x-auto border border-gray-100 rounded-2xl">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 text-gray-500 uppercase text-xs tracking-wider bg-gray-50">
                      <th className="py-3 px-4 font-semibold">Equipment</th>
                      <th className="py-3 px-4 font-semibold">Type</th>
                      <th className="py-3 px-4 font-semibold">Priority</th>
                      <th className="py-3 px-4 font-semibold">Status</th>
                      <th className="py-3 px-4 font-semibold">Scheduled</th>
                      <th className="py-3 px-4 font-semibold">Downtime Hr</th>
                      <th className="py-3 px-4 font-semibold">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(Array.isArray(maintenanceTasks) ? maintenanceTasks : [])
                      .filter(t => workOrderStatusFilter === 'ALL' || t.status === workOrderStatusFilter)
                      .map(task => (
                        <tr key={task.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                          <td className="py-3 px-4 font-medium text-gray-800">{task.equipmentName}</td>
                          <td className="py-3 px-4 text-gray-600">{task.taskType}</td>
                          <td className="py-3 px-4">
                            <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                              task.priority === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                              task.priority === 'HIGH' ? 'bg-orange-100 text-orange-700' :
                              task.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                              'bg-gray-100 text-gray-600'
                            }`}>{task.priority}</span>
                          </td>
                          <td className="py-3 px-4">
                            <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                              task.status === 'COMPLETED' || task.status === 'VERIFIED' ? 'bg-green-100 text-green-700' :
                              task.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-700' :
                              task.status === 'ASSIGNED' ? 'bg-purple-100 text-purple-700' :
                              'bg-yellow-100 text-yellow-700'
                            }`}>{task.status?.replace('_',' ')}</span>
                          </td>
                          <td className="py-3 px-4 text-gray-600">{task.scheduledDate ? new Date(task.scheduledDate).toLocaleDateString() : '-'}</td>
                          <td className="py-3 px-4 text-gray-600">{task.downtimeHours ?? '-'}</td>
                          <td className="py-3 px-4">
                            {task.status !== 'VERIFIED' && (
                              <button
                                onClick={() => { setWorkOrderTransition({id: task.id, currentStatus: task.status, targetStatus: '', notes: '', downtimeHours: ''}); setShowTransitionModal(true); }}
                                className="text-xs bg-indigo-50 text-indigo-700 hover:bg-indigo-100 px-3 py-1.5 rounded-lg font-semibold transition-colors">
                                Update Status
                              </button>
                            )}
                          </td>
                        </tr>
                    ))}
                    {(maintenanceTasks || []).filter(t => workOrderStatusFilter === 'ALL' || t.status === workOrderStatusFilter).length === 0 && (
                      <tr><td colSpan="7" className="py-8 text-center text-gray-400 bg-gray-50">No work orders match the filter.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* — Module 2 Enhancement: Calibration Compliance Dashboard —————————————————— */}
          {canManage && (
            <div className="mt-10">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold flex items-center gap-2 text-gray-800">
                  <CheckCircle size={22} className="text-teal-500" /> Calibration Compliance
                </h2>
                <button onClick={() => setShowCalibrationForm(v => !v)}
                  className="flex items-center gap-2 bg-teal-600 hover:bg-teal-700 text-white px-4 py-2 rounded-xl text-sm font-semibold transition-colors shadow-sm">
                  <Plus size={16} /> {showCalibrationForm ? 'Cancel' : 'Log Calibration'}
                </button>
              </div>

              {showCalibrationForm && (
                <div className="bg-teal-50 border border-teal-100 rounded-2xl p-6 mb-6 animate-fade-in">
                  <h3 className="font-semibold text-teal-800 mb-4">Log New Calibration Record</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <select value={calibrationForm.equipmentId} onChange={e => setCalibrationForm(f => ({...f, equipmentId: e.target.value}))}
                      className="border border-teal-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-teal-400">
                      <option value="">Select Equipment *</option>
                      {equipmentList.map(eq => <option key={eq.id} value={eq.id}>{eq.name}</option>)}
                    </select>
                    <input type={calibrationForm.calibrationDate ? "date" : "text"} onFocus={e => e.target.type = 'date'} onBlur={e => e.target.type = calibrationForm.calibrationDate ? 'date' : 'text'}
                      value={calibrationForm.calibrationDate} onChange={e => setCalibrationForm(f => ({...f, calibrationDate: e.target.value}))}
                      placeholder="Calibration Date *" className="border border-teal-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-teal-400" />
                    <input type={calibrationForm.expiryDate ? "date" : "text"} onFocus={e => e.target.type = 'date'} onBlur={e => e.target.type = calibrationForm.expiryDate ? 'date' : 'text'}
                      value={calibrationForm.expiryDate} onChange={e => setCalibrationForm(f => ({...f, expiryDate: e.target.value}))}
                      placeholder="Next Due (Expiry) Date *" className="border border-teal-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-teal-400" />
                    <input type="text" value={calibrationForm.vendorName} onChange={e => setCalibrationForm(f => ({...f, vendorName: e.target.value}))}
                      placeholder="Vendor / Lab Name" className="border border-teal-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-teal-400" />
                    <select value={calibrationForm.status} onChange={e => setCalibrationForm(f => ({...f, status: e.target.value}))}
                      className="border border-teal-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-teal-400">
                      <option value="VALID">VALID</option>
                      <option value="FAILED">FAILED</option>
                      <option value="EXPIRED">EXPIRED</option>
                    </select>
                    <input type="url" value={calibrationForm.certificateUrl} onChange={e => setCalibrationForm(f => ({...f, certificateUrl: e.target.value}))}
                      placeholder="Certificate URL" className="border border-teal-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-teal-400" />
                    <textarea value={calibrationForm.toleranceMetrics} onChange={e => setCalibrationForm(f => ({...f, toleranceMetrics: e.target.value}))}
                      placeholder='Tolerance Metrics JSON (e.g. {"accuracy": "±0.02%"})'
                      className="border border-teal-200 rounded-xl px-4 py-2.5 bg-white text-sm focus:outline-none focus:border-teal-400 md:col-span-2" rows={2} />
                  </div>
                  <div className="flex justify-end mt-4">
                    <button onClick={handleLogCalibration} disabled={isSubmittingCalibration}
                      className="bg-teal-600 hover:bg-teal-700 text-white px-6 py-2.5 rounded-xl font-semibold text-sm disabled:opacity-60 flex items-center gap-2">
                      {isSubmittingCalibration ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle size={14} />}
                      Save Calibration
                    </button>
                  </div>
                </div>
              )}

              {isFetchingCompliance ? (
                <div className="flex justify-center py-8"><Loader2 size={24} className="animate-spin text-teal-500" /></div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {complianceDashboard.map((item, i) => {
                    const daysLeft = item.daysUntilExpiry ?? 999;
                    const urgency = daysLeft <= 7 ? 'CRITICAL' : daysLeft <= 14 ? 'HIGH' : daysLeft <= 30 ? 'WARNING' : 'OK';
                    const colors = { CRITICAL: 'border-red-400 bg-red-50', HIGH: 'border-orange-400 bg-orange-50', WARNING: 'border-yellow-400 bg-yellow-50', OK: 'border-green-400 bg-green-50' };
                    const badges = { CRITICAL: 'bg-red-100 text-red-700', HIGH: 'bg-orange-100 text-orange-700', WARNING: 'bg-yellow-100 text-yellow-700', OK: 'bg-green-100 text-green-700' };
                    return (
                      <div key={i} className={`border-l-4 rounded-2xl p-5 ${colors[urgency]}`}>
                        <div className="flex items-center justify-between mb-3">
                          <div className="font-bold text-gray-800 text-sm">{item.equipmentName}</div>
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${badges[urgency]}`}>{urgency}</span>
                        </div>
                        <div className="text-2xl font-black text-gray-700">{daysLeft < 999 ? daysLeft : '—'}<span className="text-sm font-medium text-gray-500 ml-1">days left</span></div>
                        <div className="text-xs text-gray-500 mt-2">Expires: {item.expiryDate ? new Date(item.expiryDate).toLocaleDateString() : 'N/A'}</div>
                        <div className="text-xs text-gray-500">Vendor: {item.vendorName || 'N/A'}</div>
                        <span className={`mt-2 inline-block px-2 py-0.5 rounded text-[10px] font-bold ${item.lastResult === 'VALID' ? 'bg-green-100 text-green-700' : item.lastResult === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600'}`}>
                          Last: {item.lastResult || 'N/A'}
                        </span>
                      </div>
                    );
                  })}
                  {complianceDashboard.length === 0 && (
                    <div className="col-span-3 py-8 text-center text-gray-400 bg-gray-50 rounded-2xl border border-dashed border-gray-200">
                      No compliance data available.
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </section>
      )}

      {/* — Work Order Status Transition Modal —————————————————————————————— */}
      {showTransitionModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4" onClick={() => setShowTransitionModal(false)}>
          <div className="bg-white rounded-2xl p-8 max-w-md w-full shadow-2xl" onClick={e => e.stopPropagation()}>
            <h3 className="text-xl font-bold mb-4 text-gray-800">Update Work Order Status</h3>
            <div className="flex flex-col gap-3">
              <select value={workOrderTransition.targetStatus} onChange={e => setWorkOrderTransition(t => ({...t, targetStatus: e.target.value}))}
                className="border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-indigo-400">
                <option value="">Select Next Status *</option>
                {workOrderTransition.currentStatus === 'CREATED' && <option value="ASSIGNED">ASSIGNED</option>}
                {workOrderTransition.currentStatus === 'ASSIGNED' && <option value="IN_PROGRESS">IN_PROGRESS</option>}
                {workOrderTransition.currentStatus === 'IN_PROGRESS' && <option value="COMPLETED">COMPLETED</option>}
                {workOrderTransition.currentStatus === 'COMPLETED' && <option value="VERIFIED">VERIFIED</option>}
                <option value="CANCELLED">CANCELLED</option>
              </select>
              <input type="number" min="0" step="0.5" placeholder="Downtime Hours (if any)"
                value={workOrderTransition.downtimeHours} onChange={e => setWorkOrderTransition(t => ({...t, downtimeHours: e.target.value}))}
                className="border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-indigo-400" />
              <textarea placeholder="Resolution Notes" value={workOrderTransition.notes} onChange={e => setWorkOrderTransition(t => ({...t, notes: e.target.value}))}
                className="border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-indigo-400" rows={3} />
              <div className="flex gap-3 justify-end mt-2">
                <button onClick={() => setShowTransitionModal(false)} className="px-5 py-2.5 rounded-xl border border-gray-200 text-gray-600 text-sm font-semibold hover:bg-gray-50">Cancel</button>
                <button onClick={handleWorkOrderTransition} className="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold">Update Status</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* — Module 5: OEE Reports Section —————————————————————————————————————— */}
      {activeSection === 'reports' && (
        <section className="flex flex-col gap-6 animate-fade-in">
          {/* Tab switcher */}
          <div className="bg-white rounded-2xl p-2 shadow-sm border border-gray-100 flex gap-2 w-fit">
            {['generate', 'results'].map(tab => (
              <button key={tab} onClick={() => setActiveReportTab(tab)}
                className={`px-5 py-2 rounded-xl text-sm font-semibold transition-all capitalize ${activeReportTab === tab ? 'bg-brand-orange text-white shadow-sm' : 'text-gray-600 hover:bg-gray-100'}`}>
                {tab === 'generate' ? '⚙️ Generate Report' : '📊 Results'}
              </button>
            ))}
          </div>

          {activeReportTab === 'generate' && (
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100">
              <h2 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
                <Activity size={22} className="text-brand-orange" /> OEE Report Configuration
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-w-lg">
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wide">From Date *</label>
                  <input type="date" value={reportForm.from} onChange={e => setReportForm(f => ({...f, from: e.target.value}))}
                    className="border border-gray-200 rounded-xl px-4 py-2.5 text-sm w-full focus:outline-none focus:border-brand-orange" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wide">To Date *</label>
                  <input type="date" value={reportForm.to} onChange={e => setReportForm(f => ({...f, to: e.target.value}))}
                    className="border border-gray-200 rounded-xl px-4 py-2.5 text-sm w-full focus:outline-none focus:border-brand-orange" />
                </div>
              </div>
              <p className="text-xs text-gray-400 mt-3">Leave equipment selection empty to include all equipment. Report generates asynchronously.</p>

              <div className="mt-6 flex items-center gap-4">
                <button onClick={handleGenerateReport} disabled={isGeneratingReport}
                  className="bg-brand-orange hover:bg-orange-600 text-white px-6 py-2.5 rounded-xl font-semibold text-sm disabled:opacity-60 flex items-center gap-2 shadow-sm">
                  {isGeneratingReport ? <><Loader2 size={14} className="animate-spin" /> Generating...</> : <><Activity size={14} /> Generate OEE Report</>}
                </button>
                {reportId && (
                  <span className={`text-sm font-semibold px-3 py-1.5 rounded-lg ${
                    reportStatus === 'DONE' ? 'bg-green-100 text-green-700' :
                    reportStatus === 'PENDING' ? 'bg-amber-100 text-amber-700' :
                    reportStatus === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600'
                  }`}>Status: {reportStatus}</span>
                )}
              </div>

              {reportId && reportStatus === 'DONE' && (
                <div className="mt-5 flex gap-3">
                  <button
                    onClick={async () => {
                      try {
                        const res = await api.get(`/reports/${reportId}/download?format=csv`, { responseType: 'blob' });
                        const url = URL.createObjectURL(new Blob([res.data], { type: 'text/csv' }));
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = `oee-report-${reportForm.from}-to-${reportForm.to}.csv`;
                        document.body.appendChild(a);
                        a.click();
                        document.body.removeChild(a);
                        URL.revokeObjectURL(url);
                      } catch (err) {
                        toast.error('CSV download failed: ' + (err.response?.data?.message || err.message));
                      }
                    }}
                    className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors shadow-sm"
                  >
                    <FileText size={14} /> Download CSV
                  </button>
                  <button
                    onClick={async () => {
                      try {
                        const res = await api.get(`/reports/${reportId}/download?format=pdf`, { responseType: 'blob' });
                        const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = `oee-report-${reportForm.from}-to-${reportForm.to}.pdf`;
                        document.body.appendChild(a);
                        a.click();
                        document.body.removeChild(a);
                        URL.revokeObjectURL(url);
                      } catch (err) {
                        toast.error('PDF download failed: ' + (err.response?.data?.message || err.message));
                      }
                    }}
                    className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors shadow-sm"
                  >
                    <FileText size={14} /> Download PDF
                  </button>
                </div>
              )}
            </div>
          )}

          {activeReportTab === 'results' && (
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100">
              <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center gap-2">
                <CheckCircle size={22} className="text-green-500" /> OEE Report Results
              </h2>
              {reportResult && reportResult.entries ? (
                <>
                  <p className="text-sm text-gray-500 mb-6">
                    Period: <strong>{reportResult.from}</strong> → <strong>{reportResult.to}</strong> &nbsp;|&nbsp;
                    Generated: <strong>{new Date(reportResult.generatedAt).toLocaleString()}</strong> &nbsp;|&nbsp;
                    <span className="text-xs text-gray-400">{reportResult.entries.length} equipment records</span>
                  </p>
                  <div className="overflow-x-auto border border-gray-100 rounded-2xl">
                    <table className="w-full text-left text-xs">
                      <thead>
                        <tr className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white">
                          {['Equipment','Sched. Hr','Usage Hr','Down Hr','Total Bk','Conf. Bk','Avail%','Perf%','Qual%','OEE%','₹/Hr','Idle ₹'].map(h => (
                            <th key={h} className="py-3 px-3 font-semibold whitespace-nowrap">{h}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {reportResult.entries.map((e, i) => {
                          const oee = e.oeeScore * 100;
                          const rowColor = oee >= 65 ? 'bg-green-50' : oee >= 40 ? 'bg-yellow-50' : 'bg-red-50';
                          return (
                            <tr key={i} className={`border-b border-gray-100 ${rowColor} hover:brightness-95 transition-all`}>
                              <td className="py-3 px-3 font-medium text-gray-800 whitespace-nowrap">{e.equipmentName}</td>
                              <td className="py-3 px-3 text-gray-600">{e.scheduledHours?.toFixed(0)}</td>
                              <td className="py-3 px-3 text-gray-600">{e.usageHours?.toFixed(1)}</td>
                              <td className="py-3 px-3 text-gray-600">{e.downtimeHours?.toFixed(1)}</td>
                              <td className="py-3 px-3 text-gray-600">{e.totalBookings}</td>
                              <td className="py-3 px-3 text-gray-600">{e.confirmedBookings}</td>
                              <td className="py-3 px-3 font-semibold">{(e.availability * 100).toFixed(1)}%</td>
                              <td className="py-3 px-3 font-semibold">{(e.performance * 100).toFixed(1)}%</td>
                              <td className="py-3 px-3 font-semibold">{(e.quality * 100).toFixed(1)}%</td>
                              <td className={`py-3 px-3 font-black text-base ${oee >= 65 ? 'text-green-700' : oee >= 40 ? 'text-yellow-700' : 'text-red-700'}`}>
                                {oee.toFixed(1)}%
                              </td>
                              <td className="py-3 px-3 text-gray-600">₹{e.pricePerHour}</td>
                              <td className="py-3 px-3 text-gray-600 font-semibold">₹{e.idleCost}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                  <div className="mt-4 flex gap-6 text-xs text-gray-500">
                    <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-full bg-green-400 inline-block" /> OEE ≥ 65% (World-Class)</span>
                    <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-full bg-yellow-400 inline-block" /> 40–64% (Average)</span>
                    <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-full bg-red-400 inline-block" /> &lt;40% (Low)</span>
                  </div>
                </>
              ) : (
                <div className="py-16 text-center text-gray-400">
                  <Activity size={40} className="mx-auto mb-3 text-gray-300" />
                  <p className="font-medium">No report generated yet.</p>
                  <p className="text-sm mt-1">Switch to the Generate tab, select a date range, and click Generate.</p>
                </div>
              )}
            </div>
          )}
        </section>
      )}

      {activeSection === 'billing' && (
        <section className="flex flex-col gap-6 animate-fade-in">

          {/* — Module 3 Enhancement: Auto-Invoice + Approval UI —————————————————— */}
          {(hasRole('LAB_MANAGER') || hasRole('DEPT_HEAD') || hasRole('INSTITUTION_ADMIN')) && selectedDepartment && (
            <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100">
              <div className="flex items-center justify-between">
                <h3 className="font-bold text-gray-800 flex items-center gap-2">
                  <ShoppingCart size={18} className="text-purple-500" /> Auto-Invoice Generator
                </h3>
                <button onClick={() => setShowAutoInvoiceForm(v => !v)}
                  className="flex items-center gap-2 bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-xl text-sm font-semibold transition-colors shadow-sm">
                  <Plus size={16} /> {showAutoInvoiceForm ? 'Cancel' : 'Generate Auto-Invoice'}
                </button>
              </div>
              {showAutoInvoiceForm && (
                <div className="mt-5 grid grid-cols-1 md:grid-cols-3 gap-4 animate-fade-in">
                  <input type="date" value={autoInvoiceForm.periodStart} onChange={e => setAutoInvoiceForm(f => ({...f, periodStart: e.target.value}))}
                    placeholder="Period Start *" className="border border-purple-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-purple-400" />
                  <input type="date" value={autoInvoiceForm.periodEnd} onChange={e => setAutoInvoiceForm(f => ({...f, periodEnd: e.target.value}))}
                    placeholder="Period End *" className="border border-purple-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-purple-400" />
                  <select value={autoInvoiceForm.fundingSourceId} onChange={e => setAutoInvoiceForm(f => ({...f, fundingSourceId: e.target.value}))}
                    className="border border-purple-200 rounded-xl px-4 py-2.5 text-sm bg-white focus:outline-none focus:border-purple-400">
                    <option value="">Funding Source (optional)</option>
                    {fundingSources.map(fs => <option key={fs.id} value={fs.id}>{fs.name}</option>)}
                  </select>
                  <div className="md:col-span-3 flex justify-end">
                    <button onClick={handleGenerateAutoInvoice} className="bg-purple-600 hover:bg-purple-700 text-white px-6 py-2.5 rounded-xl font-semibold text-sm flex items-center gap-2">
                      <FileText size={14} /> Generate Invoice
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Billing Header */}
          <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
              <div>
                <h2 className="text-xl font-bold flex items-center gap-2 text-gray-800">
                  <FileText size={24} className="text-brand-orange" />
                  {isSystemAdmin ? 'Platform Billing Overview' : 'Billing & Invoices'}
                </h2>
                <p className="text-sm text-gray-500 mt-1">
                  {isSystemAdmin ? 'Read-only view of all platform invoices and transactions.' : 'View your invoices and payment history.'}
                </p>
              </div>
              {isSystemAdmin && (
                <div className="flex items-center gap-2 text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200 px-4 py-2 rounded-xl">
                  <Shield size={14} /> Admin View — Payments are managed by institution members
                </div>
              )}
            </div>

            {/* Filter Bar */}
            <div className="flex flex-wrap items-center gap-3 mt-5 pt-5 border-t border-gray-100">
              {/* Institution filter (System Admin only) */}
              {isSystemAdmin && (
                <select
                  value={billingInstitutionFilter}
                  onChange={(e) => { setBillingInstitutionFilter(e.target.value); setInvoiceCurrentPage(0); }}
                  className="border border-gray-200 rounded-xl px-3 py-2 text-sm text-gray-700 focus:outline-none focus:border-brand-orange bg-white min-w-[180px]"
                >
                  <option value="">All Institutions</option>
                  {institutions.map(inst => (
                    <option key={inst.id} value={inst.name}>{inst.name}</option>
                  ))}
                </select>
              )}
              {/* Search */}
              <div className="relative flex-1 min-w-[200px]">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                <input
                  type="text"
                  placeholder="Search by ID, equipment, or user..."
                  value={invoiceSearchQuery}
                  onChange={(e) => { setInvoiceSearchQuery(e.target.value); setInvoiceCurrentPage(0); }}
                  className="w-full pl-9 pr-4 py-2 rounded-xl border border-gray-200 focus:outline-none focus:border-brand-orange text-sm transition-colors"
                />
              </div>
              {/* Status filter */}
              <div className="flex items-center gap-2 flex-wrap">
                {['ALL', 'PENDING_APPROVAL', 'APPROVED', 'ISSUED', 'PAID', 'CANCELLED', 'OVERDUE'].map(s => (
                  <button
                    key={s}
                    onClick={() => { setInvoiceStatusFilter(s); setInvoiceCurrentPage(0); }}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                      invoiceStatusFilter === s ? 'bg-brand-orange text-white shadow-sm' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {s === 'ALL' ? 'All Statuses' : s}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Invoices Table */}
          <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100">
            <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2 text-base">
              <FileText size={18} className="text-brand-orange" /> Invoices
              <span className="ml-auto text-xs font-medium text-gray-400 bg-gray-100 px-3 py-1 rounded-full">{filteredInvoices.length} records</span>
            </h3>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-gray-500 uppercase text-xs tracking-wider">
                    <th className="pb-3 font-semibold">Invoice #</th>
                    {isSystemAdmin && <th className="pb-3 font-semibold">Institution</th>}
                    <th className="pb-3 font-semibold">Details</th>
                    <th className="pb-3 font-semibold">Amount</th>
                    <th className="pb-3 font-semibold">Status</th>
                    <th className="pb-3 font-semibold">Date</th>
                    {!isSystemAdmin && <th className="pb-3 font-semibold text-right">Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {filteredInvoices
                    .slice(invoiceCurrentPage * INVOICE_PAGE_SIZE, (invoiceCurrentPage + 1) * INVOICE_PAGE_SIZE)
                    .map(invoice => (
                    <tr key={invoice.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors cursor-pointer" onClick={() => { setSelectedInvoiceDetails(invoice); setIsInvoiceViewOpen(true); }}>
                      <td className="py-4 font-medium text-gray-800 text-xs font-mono" title={invoice.id}>INV-{invoice.id?.substring(0, 8).toUpperCase()}</td>
                      {isSystemAdmin && (
                        <td className="py-4 text-gray-600 text-xs">{invoice.billedToInstitutionName || '—'}</td>
                      )}
                      <td className="py-4 text-gray-600 text-xs">
                        {invoice.lineItems && invoice.lineItems.length > 0 ? invoice.lineItems[0].equipmentName || invoice.lineItems[0].description : invoice.bookingId}
                      </td>
                      <td className="py-4 font-medium text-gray-900">₹{invoice.totalAmount}</td>
                      <td className="py-4">
                        <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                          invoice.status === 'PAID' ? 'bg-green-100 text-green-700' :
                          invoice.status === 'ISSUED' ? 'bg-amber-100 text-amber-700' :
                          invoice.status === 'OVERDUE' ? 'bg-red-100 text-red-700' :
                          invoice.status === 'PENDING_APPROVAL' ? 'bg-purple-100 text-purple-700' :
                          invoice.status === 'APPROVED' ? 'bg-blue-100 text-blue-700' :
                          'bg-gray-100 text-gray-600'
                        }`}>{invoice.status?.replace('_',' ')}</span>
                      </td>
                      <td className="py-4 text-gray-600">{invoice.invoiceDate ? new Date(invoice.invoiceDate).toLocaleDateString() : (invoice.generatedDate ? new Date(invoice.generatedDate).toLocaleDateString() : 'N/A')}</td>
                      {/* Actions column */}
                      {!isSystemAdmin && (
                        <td className="py-4 text-right" onClick={e => e.stopPropagation()}>
                          <div className="flex items-center justify-end gap-2">
                            {/* Approval actions for managers */}
                            {invoice.status === 'PENDING_APPROVAL' && (hasRole('INSTITUTION_ADMIN') || hasRole('DEPT_HEAD')) && (
                              <>
                                <button
                                  onClick={async (e) => {
                                    e.stopPropagation();
                                    setIsApprovingInvoice(true);
                                    try {
                                      await api.patch(`/billing/invoices/${invoice.id}/approve`);
                                      fetchInvoices();
                                    } catch(err) { alert('Failed to approve: ' + (err.response?.data?.message || err.message)); }
                                    finally { setIsApprovingInvoice(false); }
                                  }}
                                  className="bg-green-600 hover:bg-green-700 text-white px-3 py-1.5 rounded-lg text-xs font-bold transition-colors flex items-center gap-1">
                                  <CheckCircle size={12} /> Approve
                                </button>
                                <button
                                  onClick={async (e) => {
                                    e.stopPropagation();
                                    if (!window.confirm('Reject this invoice?')) return;
                                    try {
                                      await api.patch(`/billing/invoices/${invoice.id}/reject`);
                                      fetchInvoices();
                                    } catch(err) { alert('Failed: ' + (err.response?.data?.message || err.message)); }
                                  }}
                                  className="bg-red-100 hover:bg-red-200 text-red-700 px-3 py-1.5 rounded-lg text-xs font-bold transition-colors flex items-center gap-1">
                                  <X size={12} /> Reject
                                </button>
                              </>
                            )}
                            {/* Payment action */}
                            {invoice.status === 'ISSUED' && (
                              <button
                                onClick={(e) => { e.stopPropagation(); setSelectedInvoiceToPay(invoice); setIsPaymentModalOpen(true); }}
                                className="bg-brand-orange hover:bg-orange-600 text-white px-4 py-1.5 rounded-full text-sm font-medium transition-colors">
                                Pay Now
                              </button>
                            )}
                          </div>
                        </td>
                      )}
                    </tr>
                  ))}
                  {filteredInvoices.length === 0 && (
                    <tr><td colSpan={isSystemAdmin ? 6 : 6} className="py-8 text-center text-gray-500 bg-gray-50 rounded-xl">
                      {(!invoices || invoices.length === 0) ? 'No invoices found.' : 'No invoices match your filters.'}
                    </td></tr>
                  )}
                </tbody>
              </table>
            </div>
            {/* Invoice Pagination */}
            {Math.ceil(filteredInvoices.length / INVOICE_PAGE_SIZE) > 1 && (
              <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-100">
                <span className="text-xs text-gray-500">
                  Showing {invoiceCurrentPage * INVOICE_PAGE_SIZE + 1}–{Math.min((invoiceCurrentPage + 1) * INVOICE_PAGE_SIZE, filteredInvoices.length)} of {filteredInvoices.length}
                </span>
                <div className="flex items-center gap-1">
                  <button disabled={invoiceCurrentPage === 0} onClick={() => setInvoiceCurrentPage(p => p - 1)} className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 disabled:opacity-40 transition-colors">← Prev</button>
                  {Array.from({ length: Math.min(Math.ceil(filteredInvoices.length / INVOICE_PAGE_SIZE), 5) }, (_, i) => {
                    const tp = Math.ceil(filteredInvoices.length / INVOICE_PAGE_SIZE);
                    const page = tp <= 5 ? i : Math.max(0, Math.min(invoiceCurrentPage - 2, tp - 5)) + i;
                    return <button key={page} onClick={() => setInvoiceCurrentPage(page)} className={`w-8 h-8 text-xs font-semibold rounded-lg transition-colors ${invoiceCurrentPage === page ? 'bg-brand-orange text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>{page + 1}</button>;
                  })}
                  <button disabled={invoiceCurrentPage >= Math.ceil(filteredInvoices.length / INVOICE_PAGE_SIZE) - 1} onClick={() => setInvoiceCurrentPage(p => p + 1)} className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 disabled:opacity-40 transition-colors">Next →</button>
                </div>
              </div>
            )}
          </div>

          {/* Department Budgets (non-admin) */}
          {selectedDepartment && !isSystemAdmin && (
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100">
              <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2"><Activity size={18} className="text-brand-orange" /> Department Budgets</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {(Array.isArray(budgets) ? budgets : []).map(budget => (
                  <div key={budget.id} className="bg-gray-50 p-6 rounded-2xl border border-gray-200">
                    <div className="flex justify-between items-center mb-4">
                      <h3 className="font-bold text-gray-800">Fiscal Year {budget.fiscalYear}</h3>
                      <span className="text-xs bg-brand-orange text-white px-2 py-1 rounded-lg">Budget</span>
                    </div>
                    <div className="space-y-3">
                      <div className="flex justify-between"><span className="text-gray-500 text-sm">Allocated</span><span className="font-semibold text-gray-900">₹{budget.allocatedAmount}</span></div>
                      <div className="flex justify-between"><span className="text-gray-500 text-sm">Spent</span><span className="font-semibold text-red-600">₹{budget.spentAmount}</span></div>
                      <div className="flex justify-between pt-2 border-t border-gray-200"><span className="text-gray-800 font-bold text-sm">Remaining</span><span className="font-bold text-green-600">₹{(budget.allocatedAmount - budget.spentAmount).toFixed(2)}</span></div>
                    </div>
                  </div>
                ))}
                {(!budgets || budgets.length === 0) && <p className="col-span-full py-8 text-center text-gray-500 bg-gray-50 rounded-xl">No budgets found for this department.</p>}
              </div>
            </div>
          )}

          {/* Transactions Table */}
          <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100">
            <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2 text-base">
              <Activity size={18} className="text-brand-orange" /> {isSystemAdmin ? 'All Transactions' : 'Recent Transactions'}
              <span className="ml-auto text-xs font-medium text-gray-400 bg-gray-100 px-3 py-1 rounded-full">{allTransactions.length} records</span>
            </h3>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-gray-500 uppercase text-xs tracking-wider">
                    <th className="pb-3 font-semibold">Transaction ID</th>
                    <th className="pb-3 font-semibold">Invoice #</th>
                    <th className="pb-3 font-semibold">Date</th>
                    <th className="pb-3 font-semibold">Method</th>
                    <th className="pb-3 font-semibold">Ref Number</th>
                    <th className="pb-3 font-semibold">Amount</th>
                    <th className="pb-3 font-semibold">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {allTransactions
                    .slice(txCurrentPage * TX_PAGE_SIZE, (txCurrentPage + 1) * TX_PAGE_SIZE)
                    .map(tx => (
                      <tr key={tx?.id || Math.random()} className="border-b border-gray-50 hover:bg-gray-100 transition-colors cursor-pointer"
                        onClick={() => { setSelectedTransactionDetails(tx); setIsTransactionModalOpen(true); }}
                      >
                        <td className="py-4 font-medium text-gray-800 text-xs font-mono" title={tx?.id}>{tx?.id ? String(tx.id).substring(0, 8) : 'N/A'}...</td>
                        <td className="py-4 text-gray-500 text-xs font-mono">{tx?.invoiceId ? 'INV-' + String(tx.invoiceId).substring(0, 8).toUpperCase() : '—'}</td>
                        <td className="py-4 text-gray-600">{tx?.transactionDate ? new Date(tx.transactionDate).toLocaleDateString() : 'N/A'}</td>
                        <td className="py-4 text-gray-600">{tx?.paymentMethod || 'N/A'}</td>
                        <td className="py-4 text-gray-600">{tx?.referenceNumber || 'N/A'}</td>
                        <td className="py-4 font-medium text-gray-900">₹{tx?.amount || 0}</td>
                        <td className="py-4">
                          <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                            tx?.status === 'SUCCESS' ? 'bg-green-100 text-green-700' :
                            tx?.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-red-100 text-red-700'
                          }`}>{tx?.status || 'UNKNOWN'}</span>
                        </td>
                      </tr>
                    ))}
                  {allTransactions.length === 0 && (
                    <tr><td colSpan="7" className="py-8 text-center text-gray-500 bg-gray-50 rounded-xl">No transactions found.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            {/* Transaction Pagination */}
            {Math.ceil(allTransactions.length / TX_PAGE_SIZE) > 1 && (
              <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-100">
                <span className="text-xs text-gray-500">Showing {txCurrentPage * TX_PAGE_SIZE + 1}–{Math.min((txCurrentPage + 1) * TX_PAGE_SIZE, allTransactions.length)} of {allTransactions.length}</span>
                <div className="flex items-center gap-1">
                  <button disabled={txCurrentPage === 0} onClick={() => setTxCurrentPage(p => p - 1)} className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 disabled:opacity-40 transition-colors">← Prev</button>
                  {Array.from({ length: Math.min(Math.ceil(allTransactions.length / TX_PAGE_SIZE), 5) }, (_, i) => {
                    const tp = Math.ceil(allTransactions.length / TX_PAGE_SIZE);
                    const page = tp <= 5 ? i : Math.max(0, Math.min(txCurrentPage - 2, tp - 5)) + i;
                    return <button key={page} onClick={() => setTxCurrentPage(page)} className={`w-8 h-8 text-xs font-semibold rounded-lg transition-colors ${txCurrentPage === page ? 'bg-brand-orange text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>{page + 1}</button>;
                  })}
                  <button disabled={txCurrentPage >= Math.ceil(allTransactions.length / TX_PAGE_SIZE) - 1} onClick={() => setTxCurrentPage(p => p + 1)} className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 disabled:opacity-40 transition-colors">Next →</button>
                </div>
              </div>
            )}
          </div>

        </section>
      )}

      {activeSection === 'analytics' && (
        <section className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 animate-fade-in relative">
          <h2 className="text-xl font-bold flex items-center gap-2 mb-6 text-gray-800">
            <Activity size={24} className="text-brand-orange" />
            Analytics Summary
          </h2>
          {analyticsData ? (
            analyticsData.error ? (
              <div className="flex justify-center items-center h-48 bg-red-50 rounded-2xl border border-red-100">
                <span className="text-red-500 font-medium">{analyticsData.error}</span>
              </div>
            ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-10">
                <div 
                  className="bg-blue-50 p-6 rounded-2xl border border-blue-100 flex flex-col items-center justify-center cursor-pointer hover:bg-blue-100 hover:shadow-md transition-all duration-300 transform hover:-translate-y-1"
                  onClick={() => openAnalyticsDetails('Total Equipment', 'equipment', null)}
                >
                  <span className="text-4xl font-bold text-blue-600 mb-2">{analyticsData.totalEquipment || 0}</span>
                  <span className="text-sm font-medium text-blue-800">Total Equipment</span>
                </div>
                <div 
                  className="bg-yellow-50 p-6 rounded-2xl border border-yellow-100 flex flex-col items-center justify-center cursor-pointer hover:bg-yellow-100 hover:shadow-md transition-all duration-300 transform hover:-translate-y-1"
                  onClick={() => openAnalyticsDetails('Under Maintenance', 'equipment', 'UNDER_MAINTENANCE')}
                >
                  <span className="text-4xl font-bold text-yellow-600 mb-2">{analyticsData.underMaintenance || 0}</span>
                  <span className="text-sm font-medium text-yellow-800">Under Maintenance</span>
                </div>
                <div 
                  className="bg-green-50 p-6 rounded-2xl border border-green-100 flex flex-col items-center justify-center cursor-pointer hover:bg-green-100 hover:shadow-md transition-all duration-300 transform hover:-translate-y-1"
                  onClick={() => openAnalyticsDetails('Total Bookings', 'bookings', null)}
                >
                  <span className="text-4xl font-bold text-green-600 mb-2">{analyticsData.totalBookings || 0}</span>
                  <span className="text-sm font-medium text-green-800">Total Bookings</span>
                </div>
                <div 
                  className="bg-purple-50 p-6 rounded-2xl border border-purple-100 flex flex-col items-center justify-center cursor-pointer hover:bg-purple-100 hover:shadow-md transition-all duration-300 transform hover:-translate-y-1"
                  onClick={() => openAnalyticsDetails('Pending Approvals', 'bookings', 'PENDING')}
                >
                  <span className="text-4xl font-bold text-purple-600 mb-2">{analyticsData.pendingApprovals || 0}</span>
                  <span className="text-sm font-medium text-purple-800">Pending Approvals</span>
                </div>
              </div>

              <h3 className="text-lg font-bold text-gray-800 mb-4">Bookings per Equipment</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 text-gray-500 uppercase">
                      <th className="pb-3 font-semibold">Equipment Name</th>
                      <th className="pb-3 font-semibold text-right">Total Bookings</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(analyticsData.bookingsByEquipment || {}).map(([name, count]) => (
                      <tr key={name} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                        <td className="py-4 font-medium text-gray-800">{name}</td>
                        <td className="py-4 text-gray-600 font-bold text-right">{String(count)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
            )
          ) : (
            <div className="flex justify-center items-center h-48">
              <Loader2 className="animate-spin text-brand-orange w-8 h-8" />
            </div>
          )}
        </section>
      )}

      {/* Booking Modal */}
      {showBookingModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fade-in p-4">
          <div className="bg-white rounded-2xl p-8 max-w-md w-full shadow-xl">
            <h3 className="text-xl font-medium mb-2">Book Equipment</h3>
            <p className="text-gray-500 text-sm mb-4">You are booking: <span className="font-medium text-gray-800">{bookingData.equipmentName}</span></p>

            {bookingData.imageBase64 && (
              <div className="mb-6 flex justify-center">
                <img src={bookingData.imageBase64} alt={bookingData.equipmentName} className="h-32 object-contain rounded-lg border border-gray-200 shadow-sm" />
              </div>
            )}

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
                  type="button"
                  onClick={handleJoinWaitlist}
                  className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-5 py-2.5 rounded-full font-medium transition-colors"
                >
                  Join Waitlist
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
        </main>
      </div>

  {/* Edit Equipment Modal */}
  {showEditEquipmentModal && editingEquipment && (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fade-in" onClick={() => { setShowEditEquipmentModal(false); setEditingEquipment(null); }}>
      <div className="bg-white rounded-3xl p-8 max-w-2xl w-full shadow-2xl relative" onClick={e => e.stopPropagation()}>
        <button onClick={() => { setShowEditEquipmentModal(false); setEditingEquipment(null); }} className="absolute top-6 right-6 text-gray-400 hover:text-gray-600 bg-gray-50 hover:bg-gray-100 p-2 rounded-full transition-colors"><X size={20} /></button>
        <h3 className="text-2xl font-bold mb-6 text-gray-800 flex items-center gap-3">
          <Settings className="text-brand-orange" size={28} /> Edit Equipment
        </h3>

        <form onSubmit={handleEditEquipmentSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <input type="hidden" name="categoryId" value={editingEquipment.categoryId} />
          <input type="hidden" name="departmentId" value={editingEquipment.departmentId} />
          <div className="md:col-span-2 text-sm text-gray-500 mb-2">
            Editing equipment in {editingEquipment.departmentName} ({editingEquipment.categoryName})
          </div>

          <input type="text" name="name" defaultValue={editingEquipment.name} placeholder="Equipment Name" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
          <input type="text" name="serialNumber" defaultValue={editingEquipment.serialNumber} placeholder="Serial Number" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
          <input type="text" name="manufacturer" defaultValue={editingEquipment.manufacturer} placeholder="Manufacturer" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
          <input type="text" name="modelNumber" defaultValue={editingEquipment.modelNumber} placeholder="Model Number" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
          <input type="number" step="0.01" name="pricePerHour" defaultValue={editingEquipment.pricePerHour} placeholder="Price Per Hour (₹)" required className="rounded-full border border-gray-300 px-5 py-2.5 outline-none w-full" />
          <input type="file" name="imageFile" accept="image/*" className="rounded-full border border-gray-300 px-5 py-2 outline-none w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-medium file:bg-purple-50 file:text-purple-700 hover:file:bg-purple-100" />
          <textarea name="description" defaultValue={editingEquipment.description} placeholder="Description" className="rounded-2xl border border-gray-300 px-5 py-3 outline-none w-full md:col-span-2" rows="2"></textarea>

          <div className="md:col-span-2 flex justify-end gap-3 mt-4">
            <button
              type="button"
              onClick={() => { setShowEditEquipmentModal(false); setEditingEquipment(null); }}
              className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-5 py-2.5 rounded-full font-medium transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="bg-purple-600 hover:bg-purple-700 text-white px-5 py-2.5 rounded-full font-medium transition-colors"
            >
              Save Changes
            </button>
          </div>
        </form>
      </div>
    </div>
  )}
  {/* Share Equipment Modal */}
  {showShareModal && (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fade-in p-4">
      <div className="bg-white rounded-2xl p-8 max-w-md w-full shadow-xl">
        <h3 className="text-xl font-medium mb-2">Share Equipment</h3>
        <p className="text-gray-500 text-sm mb-6">You are creating a sharing listing for: <span className="font-medium text-gray-800">{shareData.equipmentName}</span></p>

        <form onSubmit={handleShareEquipment} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Available From</label>
            <input
              type="date"
              required
              className="rounded-xl border border-gray-300 px-4 py-2.5 outline-none w-full"
              value={shareData.availableFrom}
              onChange={(e) => setShareData({ ...shareData, availableFrom: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Available To</label>
            <input
              type="date"
              required
              className="rounded-xl border border-gray-300 px-4 py-2.5 outline-none w-full"
              value={shareData.availableTo}
              onChange={(e) => setShareData({ ...shareData, availableTo: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Terms & Conditions</label>
            <textarea
              required
              placeholder="Specify any requirements, costs, or restrictions..."
              className="rounded-xl border border-gray-300 px-4 py-2.5 outline-none w-full"
              rows="3"
              value={shareData.termsAndConditions}
              onChange={(e) => setShareData({ ...shareData, termsAndConditions: e.target.value })}
            ></textarea>
          </div>

          <div className="flex justify-end gap-3 mt-4">
            <button
              type="button"
              onClick={() => setShowShareModal(false)}
              className="px-5 py-2.5 rounded-full text-gray-600 hover:bg-gray-100 font-medium transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2.5 rounded-full font-medium transition-colors"
            >
              Publish Listing
            </button>
          </div>
        </form>
      </div>
    </div>
  )}
  {/* Access Request Modal */}
  {showAccessModal && (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fade-in p-4">
      <div className="bg-white rounded-2xl p-8 max-w-md w-full shadow-xl">
        <h3 className="text-xl font-medium mb-2">Request Access</h3>
        <p className="text-gray-500 text-sm mb-6">You are requesting access to: <span className="font-medium text-gray-800">{accessRequestData.equipmentName}</span></p>

        <form onSubmit={handleCreateAccessRequest} className="flex flex-col gap-4">
                    <div className="flex gap-4 mb-2">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Start Date (DD-MM-YYYY)</label>
              <input
                type="text"
                required
                placeholder="DD-MM-YYYY"
                maxLength="10"
                className="rounded-xl border border-gray-300 px-4 py-2 outline-none w-full text-sm font-mono"
                value={(() => {
                  if (!accessRequestData.requestedStart) return '';
                  if (accessRequestData.requestedStart.includes('-') && accessRequestData.requestedStart.length === 10 && accessRequestData.requestedStart.split('-')[0].length === 4) {
                    const [y, m, d] = accessRequestData.requestedStart.split('-');
                    return `${d}-${m}-${y}`;
                  }
                  return accessRequestData.requestedStart;
                })()}
                onChange={(e) => {
                  let digits = e.target.value.replace(/\D/g, '');
                  if (digits.length > 8) digits = digits.slice(0, 8);
                  let val = digits;
                  if (digits.length > 2) val = digits.slice(0, 2) + '-' + digits.slice(2);
                  if (digits.length > 4) val = val.slice(0, 5) + '-' + digits.slice(4);
                  
                  if (val.length === 10) {
                    const [d, m, y] = val.split('-');
                    setAccessRequestData({ ...accessRequestData, requestedStart: `${y}-${m}-${d}` });
                  } else {
                    setAccessRequestData({ ...accessRequestData, requestedStart: val });
                  }
                }}
              />
            </div>
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">End Date (DD-MM-YYYY)</label>
              <input
                type="text"
                required
                placeholder="DD-MM-YYYY"
                maxLength="10"
                className="rounded-xl border border-gray-300 px-4 py-2 outline-none w-full text-sm font-mono"
                value={(() => {
                  if (!accessRequestData.requestedEnd) return '';
                  if (accessRequestData.requestedEnd.includes('-') && accessRequestData.requestedEnd.length === 10 && accessRequestData.requestedEnd.split('-')[0].length === 4) {
                    const [y, m, d] = accessRequestData.requestedEnd.split('-');
                    return `${d}-${m}-${y}`;
                  }
                  return accessRequestData.requestedEnd;
                })()}
                onChange={(e) => {
                  let digits = e.target.value.replace(/\D/g, '');
                  if (digits.length > 8) digits = digits.slice(0, 8);
                  let val = digits;
                  if (digits.length > 2) val = digits.slice(0, 2) + '-' + digits.slice(2);
                  if (digits.length > 4) val = val.slice(0, 5) + '-' + digits.slice(4);
                  
                  if (val.length === 10) {
                    const [d, m, y] = val.split('-');
                    
                    const newEnd = `${y}-${m}-${d}`;
                    const hasConflict = occupiedDates.some(range => {
                      return (range.start >= accessRequestData.requestedStart && range.start <= newEnd) ||
                             (range.end >= accessRequestData.requestedStart && range.end <= newEnd);
                    });
                    if (hasConflict) {
                       toast.error("Cannot select range over booked dates.");
                       setAccessRequestData({ ...accessRequestData, requestedEnd: '' });
                       return;
                    }
                    
                    setAccessRequestData({ ...accessRequestData, requestedEnd: newEnd });
                  } else {
                    setAccessRequestData({ ...accessRequestData, requestedEnd: val });
                  }
                }}
              />
            </div>
          </div>
          
          <div className="border border-gray-200 rounded-xl overflow-hidden mb-2">
            {isFetchingOccupiedDates ? (
              <div className="flex items-center justify-center p-8 text-gray-500">
                <Loader2 size={24} className="animate-spin text-brand-orange" />
              </div>
            ) : (
              <FullCalendar
                plugins={[ dayGridPlugin, interactionPlugin ]}
                initialView="dayGridMonth"
                height={350}
                headerToolbar={{
                  left: 'prev',
                  center: 'title',
                  right: 'next'
                }}
                events={occupiedDates.map(d => ({
                  title: 'Booked',
                  start: d.start,
                  end: new Date(new Date(d.end).getTime() + 86400000).toISOString().split('T')[0],
                  classNames: ['unavailable-event'],
                  display: 'block'
                }))}
                dateClick={(info) => {
                  const clickedDate = info.dateStr;
                  if (new Date(clickedDate) < new Date(new Date().setHours(0,0,0,0))) return;
                  
                  const availFrom = accessRequestData.availableFrom ? accessRequestData.availableFrom.split('T')[0] : null;
                  const availTo = accessRequestData.availableTo ? accessRequestData.availableTo.split('T')[0] : null;
                  if (availFrom && clickedDate < availFrom) return;
                  if (availTo && clickedDate > availTo) return;
                  
                  const isUnavailable = occupiedDates.some(range => {
                    return clickedDate >= range.start && clickedDate <= range.end;
                  });
                  
                  if (isUnavailable) {
                    toast.error("This date is already booked.");
                    return;
                  }
                  
                  if (!accessRequestData.requestedStart || (accessRequestData.requestedStart && accessRequestData.requestedEnd)) {
                    setAccessRequestData({ ...accessRequestData, requestedStart: clickedDate, requestedEnd: '' });
                  } else {
                    if (clickedDate < accessRequestData.requestedStart) {
                      setAccessRequestData({ ...accessRequestData, requestedStart: clickedDate, requestedEnd: accessRequestData.requestedStart });
                    } else {
                      const hasConflict = occupiedDates.some(range => {
                        return (range.start >= accessRequestData.requestedStart && range.start <= clickedDate) ||
                               (range.end >= accessRequestData.requestedStart && range.end <= clickedDate);
                      });
                      if (hasConflict) {
                         toast.error("Cannot select range over booked dates.");
                         return;
                      }
                      setAccessRequestData({ ...accessRequestData, requestedEnd: clickedDate });
                    }
                  }
                }}
                dayCellClassNames={(arg) => {
                  const dateStr = arg.date.toISOString().split('T')[0];
                  if (dateStr < new Date().toISOString().split('T')[0]) return [];
                  
                  const availFrom = accessRequestData.availableFrom ? accessRequestData.availableFrom.split('T')[0] : null;
                  const availTo = accessRequestData.availableTo ? accessRequestData.availableTo.split('T')[0] : null;
                  if (availFrom && dateStr < availFrom) return [];
                  if (availTo && dateStr > availTo) return [];
                  
                  const isUnavailable = occupiedDates.some(range => {
                    return dateStr >= range.start && dateStr <= range.end;
                  });
                  if (isUnavailable) return [];
                  
                  if (accessRequestData.requestedStart === dateStr || accessRequestData.requestedEnd === dateStr) {
                    return ['selected-date'];
                  }
                  if (accessRequestData.requestedStart && accessRequestData.requestedEnd && dateStr > accessRequestData.requestedStart && dateStr < accessRequestData.requestedEnd) {
                    return ['selected-date', 'opacity-70'];
                  }
                  
                  return ['selectable-date'];
                }}
              />
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Justification</label>
            <textarea
              required
              placeholder="Why do you need this equipment from another institution?"
              className="rounded-xl border border-gray-300 px-4 py-2.5 outline-none w-full"
              rows="3"
              value={accessRequestData.justification}
              onChange={(e) => setAccessRequestData({ ...accessRequestData, justification: e.target.value })}
            ></textarea>
          </div>

          <div className="flex justify-end gap-3 mt-4">
            <button
              type="button"
              onClick={() => setShowAccessModal(false)}
              className="px-5 py-2.5 rounded-full text-gray-600 hover:bg-gray-100 font-medium transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="bg-brand-orange hover:bg-orange-600 text-white px-5 py-2.5 rounded-full font-medium transition-colors"
            >
              Submit Request
            </button>
          </div>
        </form>
      </div>
    </div>
  )}
  {/* Equipment Details Modal */}
  {selectedEquipmentDetails && (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-2xl p-8 max-w-lg w-full shadow-2xl relative max-h-[90vh] overflow-y-auto custom-scrollbar">
        <button onClick={() => setSelectedEquipmentDetails(null)} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 bg-gray-50 hover:bg-gray-100 p-2 rounded-full transition-colors"><X size={20} /></button>
        <h3 className="text-2xl font-bold text-gray-800 mb-6 flex items-center gap-3">
          <div className="bg-purple-50 p-2 rounded-lg text-purple-600"><Settings size={24} /></div>
          Equipment Details
        </h3>

        {selectedEquipmentDetails.imageBase64 && (
          <div className="mb-6 flex justify-center">
            <img src={selectedEquipmentDetails.imageBase64} alt={selectedEquipmentDetails.name} className="max-h-48 object-contain rounded-xl border border-gray-200 shadow-sm" />
          </div>
        )}

        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Name:</span>
            <span className="col-span-2 text-gray-800 font-medium">{selectedEquipmentDetails.name}</span>
          </div>
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Status:</span>
            <span className="col-span-2 font-semibold">
              <span className={`px-2 py-1 rounded text-xs tracking-wide ${selectedEquipmentDetails.status === 'AVAILABLE' ? 'bg-green-100 text-green-700' : selectedEquipmentDetails.status === 'BOOKED' ? 'bg-blue-100 text-blue-700' : 'bg-red-100 text-red-700'}`}>
                {selectedEquipmentDetails.status}
              </span>
            </span>
          </div>
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Serial Number:</span>
            <span className="col-span-2 text-gray-800">{selectedEquipmentDetails.serialNumber}</span>
          </div>
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Manufacturer:</span>
            <span className="col-span-2 text-gray-800">{selectedEquipmentDetails.manufacturer}</span>
          </div>
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Model Number:</span>
            <span className="col-span-2 text-gray-800">{selectedEquipmentDetails.modelNumber}</span>
          </div>
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Price Per Hour:</span>
            <span className="col-span-2 text-gray-800">₹{selectedEquipmentDetails.pricePerHour}</span>
          </div>
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Category:</span>
            <span className="col-span-2 text-gray-800">{selectedEquipmentDetails.categoryName}</span>
          </div>
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Institution:</span>
            <span className="col-span-2 text-gray-800">{selectedEquipmentDetails.institutionName}</span>
          </div>
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Department:</span>
            <span className="col-span-2 text-gray-800">{selectedEquipmentDetails.departmentName}</span>
          </div>
          {selectedEquipmentDetails.sharedAvailableFrom && selectedEquipmentDetails.sharedAvailableTo && (
            <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
              <span className="text-gray-500 font-medium col-span-1">Available Dates:</span>
              <span className="col-span-2 text-gray-800 font-medium text-brand-orange">
                {new Date(selectedEquipmentDetails.sharedAvailableFrom).toLocaleDateString()} to {new Date(selectedEquipmentDetails.sharedAvailableTo).toLocaleDateString()}
              </span>
            </div>
          )}
          <div className="grid grid-cols-1 gap-2 pt-2">
            <span className="text-gray-500 font-medium">Description:</span>
            <p className="bg-gray-50 p-4 rounded-xl text-gray-700 text-sm whitespace-pre-wrap border border-gray-100">
              {selectedEquipmentDetails.description || 'No description provided.'}
            </p>
          </div>
        </div>
      </div>
    </div>
  )}
  {/* Booking Details Modal */}
  {selectedBookingDetails && (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-2xl p-8 max-w-lg w-full shadow-2xl relative max-h-[90vh] overflow-y-auto custom-scrollbar">
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
              {new Date(selectedBookingDetails.startTime).toLocaleDateString()} {new Date(selectedBookingDetails.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          </div>

          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">End Time:</span>
            <span className="col-span-2 text-gray-800">
              {new Date(selectedBookingDetails.endTime).toLocaleDateString()} {new Date(selectedBookingDetails.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
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
  {/* Waitlist Details Modal */}
  {showWaitlistModal && selectedWaitlistDetails && (() => {
    const eqDetails = equipmentList.find(e => e.id === selectedWaitlistDetails.equipmentId);
    return (
      <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
        <div className="bg-white rounded-2xl p-8 max-w-md w-full shadow-2xl relative">
          <button onClick={() => setShowWaitlistModal(false)} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 bg-gray-50 hover:bg-gray-100 p-2 rounded-full transition-colors"><X size={20} /></button>
          <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
            <Clock size={24} className="text-brand-orange" />
            Waitlist Details
          </h3>

          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
              <span className="text-gray-500 font-medium col-span-1">Status:</span>
              <span className="col-span-2 font-semibold">
                <span className={`px-2 py-1 rounded-full text-xs font-bold tracking-wide ${selectedWaitlistDetails.status === 'ACTIVE' ? 'bg-amber-100 text-amber-700' : selectedWaitlistDetails.status === 'FULFILLED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>
                  {selectedWaitlistDetails.status}
                </span>
              </span>
            </div>

            <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
              <span className="text-gray-500 font-medium col-span-1">Equipment:</span>
              <span className="col-span-2 text-gray-800 font-medium">{selectedWaitlistDetails.equipmentName}</span>
            </div>

            {eqDetails && (
              <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
                <span className="text-gray-500 font-medium col-span-1">Details:</span>
                <span className="col-span-2 text-gray-800 text-sm">
                  <span className="block mb-1"><span className="text-gray-500">Mfr:</span> {eqDetails.manufacturer}</span>
                  <span className="block mb-1"><span className="text-gray-500">S/N:</span> {eqDetails.serialNumber}</span>
                  <span className="block mb-1"><span className="text-gray-500">Category:</span> {eqDetails.categoryName}</span>
                  <span className="block"><span className="text-gray-500">Location:</span> {eqDetails.institutionName} ({eqDetails.departmentName})</span>
                </span>
              </div>
            )}

            <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
              <span className="text-gray-500 font-medium col-span-1">Queue Position:</span>
              <span className="col-span-2 text-gray-800 font-bold">{selectedWaitlistDetails.position || '-'}</span>
            </div>

            <div className="grid grid-cols-3 gap-2 pb-3">
              <span className="text-gray-500 font-medium col-span-1">Dates:</span>
              <span className="col-span-2 text-gray-800 text-sm">
                {new Date(selectedWaitlistDetails.requestedStart).toLocaleDateString()} {new Date(selectedWaitlistDetails.requestedStart).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                <br />to<br />
                {new Date(selectedWaitlistDetails.requestedEnd).toLocaleDateString()} {new Date(selectedWaitlistDetails.requestedEnd).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </span>
            </div>

            {selectedWaitlistDetails.status === 'ACTIVE' && (
              <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-100">
                <button onClick={() => handleCancelWaitlist(selectedWaitlistDetails.id)} className="px-5 py-2.5 bg-red-50 hover:bg-red-100 text-red-600 rounded-full text-sm font-semibold transition-colors w-full">Cancel Waitlist</button>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  })()}
  {/* Incoming Request Details Modal */}
  {selectedIncomingRequest && (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-2xl p-8 max-w-lg w-full shadow-2xl relative max-h-[90vh] overflow-y-auto custom-scrollbar">
        <button onClick={() => setSelectedIncomingRequest(null)} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 bg-gray-50 hover:bg-gray-100 p-2 rounded-full transition-colors"><X size={20} /></button>
        <h3 className="text-2xl font-bold text-gray-800 mb-6 flex items-center gap-3">
          <div className="bg-blue-50 p-2 rounded-lg text-blue-600"><FileText size={24} /></div>
          Access Request Details
        </h3>

        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Status:</span>
            <span className="col-span-2 font-semibold">
              <span className={`px-2 py-1 rounded-full text-xs font-bold tracking-wide ${selectedIncomingRequest.status === 'APPROVED' ? 'bg-green-100 text-green-700' : selectedIncomingRequest.status === 'REJECTED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'}`}>
                {selectedIncomingRequest.status}
              </span>
            </span>
          </div>

          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Equipment:</span>
            <span className="col-span-2 text-gray-800 font-medium">{selectedIncomingRequest.equipmentName}</span>
          </div>

          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Requester:</span>
            <span className="col-span-2 text-gray-800">{selectedIncomingRequest.requesterName || 'N/A'}</span>
          </div>

          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Institution:</span>
            <span className="col-span-2 text-gray-800">{selectedIncomingRequest.requesterInstitutionName || 'N/A'}</span>
          </div>

          <div className="grid grid-cols-3 gap-2 border-b border-gray-100 pb-3">
            <span className="text-gray-500 font-medium col-span-1">Dates:</span>
            <span className="col-span-2 text-gray-800">
              {selectedIncomingRequest.requestedStart ? new Date(selectedIncomingRequest.requestedStart).toLocaleDateString() : 'N/A'} - {selectedIncomingRequest.requestedEnd ? new Date(selectedIncomingRequest.requestedEnd).toLocaleDateString() : 'N/A'}
            </span>
          </div>

          <div className="grid grid-cols-1 gap-2 pt-2">
            <span className="text-gray-500 font-medium">Justification:</span>
            <p className="bg-gray-50 p-4 rounded-xl text-gray-700 text-sm whitespace-pre-wrap border border-gray-100">
              {selectedIncomingRequest.justification || 'No justification provided.'}
            </p>
          </div>

          {selectedIncomingRequest.status === 'PENDING' && (
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-100">
              <button onClick={() => { handleApproveRejectRequest(selectedIncomingRequest.id, 'REJECTED'); setSelectedIncomingRequest(null); }} className="px-5 py-2.5 bg-red-50 hover:bg-red-100 text-red-600 rounded-full text-sm font-semibold transition-colors">Reject Request</button>
              <button onClick={() => { handleApproveRejectRequest(selectedIncomingRequest.id, 'APPROVED'); setSelectedIncomingRequest(null); }} className="px-5 py-2.5 bg-green-500 hover:bg-green-600 text-white rounded-full text-sm font-semibold transition-colors">Approve Request</button>
            </div>
          )}
        </div>
      </div>
    </div>
  )}
  {/* Payment Simulation Modal */}
  {isPaymentModalOpen && selectedInvoiceToPay && (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-3xl p-8 max-w-md w-full shadow-2xl relative">
        <button onClick={() => setIsPaymentModalOpen(false)} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 bg-gray-50 hover:bg-gray-100 p-2 rounded-full transition-colors"><X size={20} /></button>
        <h3 className="text-2xl font-bold text-gray-800 mb-2 flex items-center gap-3">
          <div className="bg-green-50 p-2 rounded-xl text-green-600"><FileText size={24} /></div>
          Invoice Payment
        </h3>
        <p className="text-gray-500 text-sm mb-6 pb-4 border-b border-gray-100">Simulate industry standard payment flow.</p>

        <div className="space-y-4 mb-8">
          <div className="flex justify-between items-center p-4 bg-gray-50 rounded-xl">
            <span className="text-gray-600 font-medium text-sm">Invoice #</span>
            <span className="font-bold text-gray-900 text-sm font-mono">INV-{selectedInvoiceToPay.id?.substring(0, 8).toUpperCase() || 'N/A'}</span>
          </div>

          <div className="flex justify-between items-center p-4 bg-gray-50 rounded-xl border border-gray-100">
            <span className="text-gray-600 font-medium">Total Amount</span>
            <span className="text-2xl font-black text-brand-orange">₹{selectedInvoiceToPay.totalAmount || '0.00'}</span>
          </div>

          <div className="pt-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Payment Method</label>
            <select
              className="w-full border border-gray-300 rounded-xl px-4 py-2.5 outline-none focus:border-brand-orange focus:ring-1 focus:ring-brand-orange text-sm bg-white"
              value={paymentData.paymentMethod}
              onChange={(e) => setPaymentData({ ...paymentData, paymentMethod: e.target.value })}
            >
              <option value="CREDIT_CARD">Credit Card</option>
              <option value="BANK_TRANSFER">Bank Transfer</option>
              <option value="INTERNAL_GRANT">Internal Grant</option>
              <option value="UPI">UPI (Demo)</option>
              <option value="QR_CODE">QR Payment (Demo)</option>
            </select>
          </div>
        </div>

        <div className="flex gap-3 mt-8">
          <button
            onClick={() => handlePaymentSubmit('FAILED')}
            className="flex-1 bg-red-50 hover:bg-red-100 text-red-600 py-3 rounded-xl font-bold transition-colors shadow-sm"
          >
            Simulate Failure
          </button>
          <button
            onClick={() => handlePaymentSubmit('SUCCESS')}
            className="flex-1 bg-green-500 hover:bg-green-600 text-white py-3 rounded-xl font-bold transition-colors shadow-sm shadow-green-200"
          >
            Pay Now
          </button>
        </div>
      </div>
    </div>
  )}
  {/* Real-World Invoice View Modal */}
  {isInvoiceViewOpen && selectedInvoiceDetails && (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl w-full max-w-4xl max-h-[95vh] overflow-y-auto shadow-2xl relative animate-fade-in flex flex-col">

        {/* Sticky action bar — hidden when printing */}
        <div className="sticky top-0 bg-white/90 backdrop-blur-md px-8 py-4 border-b border-slate-100 flex justify-between items-center z-10 rounded-t-3xl print:hidden">
          <div className="flex gap-3">
            <button
              onClick={() => window.print()}
              className="flex items-center gap-2 border-l-4 bg-white hover:bg-orange-50 text-orange-500 px-5 py-2.5 rounded-xl font-semibold text-sm transition-colors shadow-sm"
              style={{borderLeftColor:'#ff4500', borderTop:'1px solid #ffe4d9', borderRight:'1px solid #ffe4d9', borderBottom:'1px solid #ffe4d9'}}
            >
              <FileText size={15} /> Print / Save PDF
            </button>
          </div>
          <button
            onClick={() => { setIsInvoiceViewOpen(false); setSelectedInvoiceDetails(null); }}
            className="text-slate-400 hover:text-slate-700 transition-colors p-1 rounded-lg hover:bg-slate-100"
          >
            <X size={22} />
          </button>
        </div>

        {/* Invoice body */}
        <EnterpriseInvoiceBody inv={selectedInvoiceDetails} />
      </div>
    </div>
  )}


  {/* ─── Notification Detail Slide-in Drawer ─────────────────────────────── */}
  {selectedNotification && (() => {
    const n = selectedNotification;

    // Type-based config for the detail panel (same map as dropdown)
    const detailTypeConfig = {
      BOOKING:                { icon: <Calendar size={22}/>, bg: 'bg-blue-100',   text: 'text-blue-600',   label: 'Booking',        section: 'bookings'        },
      BOOKING_APPROVAL_REQUEST:{ icon: <Clock size={22}/>,    bg: 'bg-amber-100',  text: 'text-amber-600',  label: 'Booking Approval',section: 'bookings'        },
      EQUIPMENT:              { icon: <Server size={22}/>,   bg: 'bg-green-100',  text: 'text-green-600',  label: 'Equipment',      section: 'equipment'       },
      MAINTENANCE:            { icon: <Wrench size={22}/>,   bg: 'bg-orange-100', text: 'text-orange-600', label: 'Maintenance',    section: 'maintenance'     },
      INVOICE:                { icon: <FileText size={22}/>, bg: 'bg-purple-100', text: 'text-purple-600', label: 'Invoice',        section: 'billing'         },
      SHARING_REQUEST:        { icon: <Network size={22}/>,  bg: 'bg-teal-100',   text: 'text-teal-600',   label: 'Sharing',        section: 'shared_resources'},
      ACCESS_REQUEST:         { icon: <Key size={22}/>,      bg: 'bg-rose-100',   text: 'text-rose-600',   label: 'Access Request', section: 'shared_resources'},
      WAITLIST:               { icon: <Clock size={22}/>,    bg: 'bg-slate-100',  text: 'text-slate-600',  label: 'Waitlist',       section: 'bookings'        },
      INSTITUTION:            { icon: <Building size={22}/>, bg: 'bg-indigo-100', text: 'text-indigo-600', label: 'Institution',    section: 'institutions'    },
      DEPARTMENT:             { icon: <Network size={22}/>,  bg: 'bg-violet-100', text: 'text-violet-600', label: 'Department',     section: 'departments'     },
      CATEGORY:               { icon: <Tags size={22}/>,     bg: 'bg-lime-100',   text: 'text-lime-600',   label: 'Category',       section: 'categories'      },
      CALIBRATION:            { icon: <Activity size={22}/>, bg: 'bg-emerald-100',text: 'text-emerald-600',label: 'Calibration',    section: 'maintenance'     },
      REPORT:                 { icon: <FileText size={22}/>, bg: 'bg-sky-100',    text: 'text-sky-600',    label: 'OEE Report',     section: 'reports'         },
      USER_MANAGEMENT:        { icon: <User size={22}/>,     bg: 'bg-pink-100',   text: 'text-pink-600',   label: 'User Management',section: 'users'           },
      PROFILE:                { icon: <User size={22}/>,     bg: 'bg-gray-100',   text: 'text-gray-600',   label: 'My Profile',     section: 'profile'         },
    };
    const cfg = detailTypeConfig[n.referenceType] || {
      icon: <Bell size={22}/>, bg: 'bg-gray-100', text: 'text-gray-600',
      label: n.referenceType, section: null
    };

    const timeAgoFull = n.createdAt
      ? new Date(n.createdAt).toLocaleString('en-IN', {
          day: '2-digit', month: 'short', year: 'numeric',
          hour: '2-digit', minute: '2-digit'
        })
      : '';

    return (
      <div
        className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex justify-end"
        onClick={(e) => { if (e.target === e.currentTarget) setSelectedNotification(null); }}
        style={{ animation: 'notifSlideIn 0.2s ease-out' }}
      >
        <div className="bg-white h-full w-full max-w-md shadow-2xl flex flex-col overflow-hidden">
          {/* Drawer Header */}
          <div className="px-6 py-5 border-b border-gray-100 flex items-center justify-between bg-gradient-to-r from-gray-50 to-white shrink-0">
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-full flex items-center justify-center ${cfg.bg} ${cfg.text}`}>
                {cfg.icon}
              </div>
              <div>
                <span className={`text-xs font-bold uppercase tracking-wide ${cfg.text}`}>{cfg.label}</span>
                <p className="text-sm font-semibold text-gray-800 mt-0.5">Notification Details</p>
              </div>
            </div>
            <button
              onClick={() => setSelectedNotification(null)}
              className="p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-400 hover:text-gray-600"
            >
              <X size={20} />
            </button>
          </div>

          {/* Drawer Body */}
          <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-5">
            {/* Timestamp */}
            {timeAgoFull && (
              <div className="flex items-center gap-2 text-xs text-gray-400">
                <Clock size={12} />
                <span>{timeAgoFull}</span>
              </div>
            )}

            {/* Actor info */}
            {n.actorName && (
              <div className="flex items-center gap-3 bg-gray-50 rounded-xl px-4 py-3 border border-gray-100">
                <div className="w-8 h-8 bg-gradient-to-br from-orange-400 to-orange-600 rounded-full flex items-center justify-center text-white font-bold text-sm shrink-0">
                  {n.actorName.charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="text-xs text-gray-400 font-medium">Action by</p>
                  <p className="text-sm font-semibold text-gray-800">{n.actorName}</p>
                </div>
                {n.action && (
                  <span className="ml-auto text-[10px] font-bold uppercase tracking-wider bg-gray-200 text-gray-600 px-2 py-0.5 rounded-full">
                    {n.action.replace(/_/g, ' ')}
                  </span>
                )}
              </div>
            )}

            {/* Message */}
            <div className="bg-gray-50 rounded-2xl p-5 border border-gray-100">
              <p className="text-sm font-semibold text-gray-500 mb-2 uppercase tracking-wider">Message</p>
              <p className="text-gray-800 text-base leading-relaxed whitespace-pre-wrap">
                {n.message || n.content}
              </p>
            </div>

            {/* Booking Approval quick actions */}
            {n.referenceType === 'BOOKING_APPROVAL_REQUEST' && (
              <div className="bg-amber-50 rounded-2xl p-4 border border-amber-100">
                <p className="text-xs font-bold text-amber-700 uppercase tracking-wider mb-3">Quick Actions</p>
                <div className="flex gap-3">
                  <button onClick={async () => {
                    try {
                      await api.patch(`/bookings/${n.referenceId}/status?status=APPROVED`);
                      toast.success('Booking approved successfully!');
                      setSelectedNotification(null);
                      fetchBookings();
                    } catch (err) { toast.error('Failed to approve.'); }
                  }} className="flex-1 px-4 py-2.5 bg-green-500 hover:bg-green-600 text-white rounded-full font-semibold text-sm transition-colors">
                    ✅ Approve
                  </button>
                  <button onClick={async () => {
                    try {
                      await api.patch(`/bookings/${n.referenceId}/status?status=REJECTED`);
                      toast.error('Booking rejected.');
                      setSelectedNotification(null);
                      fetchBookings();
                    } catch (err) { toast.error('Failed to reject.'); }
                  }} className="flex-1 px-4 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-full font-semibold text-sm transition-colors">
                    ❌ Reject
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Drawer Footer */}
          <div className="px-6 py-4 border-t border-gray-100 bg-gray-50 flex items-center gap-3 shrink-0">
            {cfg.section && (
              <button
                onClick={() => {
                  setActiveSection(cfg.section);
                  setSelectedNotification(null);
                }}
                className="flex-1 flex items-center justify-center gap-2 px-5 py-2.5 bg-brand-orange hover:bg-orange-600 text-white rounded-full font-semibold text-sm transition-colors"
              >
                <Activity size={14} />
                Go to {cfg.label}
              </button>
            )}
            <button
              onClick={() => setSelectedNotification(null)}
              className="px-5 py-2.5 bg-white border border-gray-200 hover:bg-gray-50 text-gray-700 rounded-full font-semibold text-sm transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    );
  })()}



  {isTransactionModalOpen && selectedTransactionDetails && (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex justify-center items-center z-50 p-4 animate-fade-in" onClick={(e) => { if (e.target === e.currentTarget) setIsTransactionModalOpen(false); }}>
      <div className="bg-white rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden flex flex-col transform transition-all">
        <div className="p-6 md:p-8 bg-gradient-to-r from-gray-50 to-white border-b border-gray-100 flex justify-between items-center relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-brand-orange/5 rounded-bl-full -mr-16 -mt-16 pointer-events-none"></div>
          <div>
            <h2 className="text-2xl font-bold text-gray-800">Transaction Details</h2>
            <p className="text-gray-500 text-sm mt-1">ID: {selectedTransactionDetails.id}</p>
          </div>
          <button onClick={() => setIsTransactionModalOpen(false)} className="text-gray-400 hover:text-gray-600 bg-white hover:bg-gray-100 p-2 rounded-full transition-colors border border-gray-100 shadow-sm relative z-10">
            <X size={24} />
          </button>
        </div>
        <div className="p-6 md:p-8 space-y-6 flex-1 overflow-y-auto max-h-[70vh]">
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
              <span className="text-gray-500 text-xs font-bold uppercase tracking-wider block mb-1">Date</span>
              <span className="font-semibold text-gray-900">{selectedTransactionDetails.transactionDate ? new Date(selectedTransactionDetails.transactionDate).toLocaleString() : 'N/A'}</span>
            </div>
            <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
              <span className="text-gray-500 text-xs font-bold uppercase tracking-wider block mb-1">Status</span>
              <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold ${
                selectedTransactionDetails.status === 'SUCCESS' ? 'bg-green-100 text-green-700' :
                selectedTransactionDetails.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' :
                'bg-red-100 text-red-700'
              }`}>
                {selectedTransactionDetails.status || 'UNKNOWN'}
              </span>
            </div>
            <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
              <span className="text-gray-500 text-xs font-bold uppercase tracking-wider block mb-1">Amount</span>
              <span className="font-semibold text-gray-900">₹{selectedTransactionDetails.amount || 0}</span>
            </div>
            <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
              <span className="text-gray-500 text-xs font-bold uppercase tracking-wider block mb-1">Payment Method</span>
              <span className="font-semibold text-gray-900">{selectedTransactionDetails.paymentMethod || 'N/A'}</span>
            </div>
          </div>
          <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
            <span className="text-gray-500 text-xs font-bold uppercase tracking-wider block mb-1">Reference Number</span>
            <span className="font-semibold text-gray-900 break-all">{selectedTransactionDetails.referenceNumber || 'N/A'}</span>
          </div>
          {selectedTransactionDetails.invoiceId && (
            <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
              <span className="text-gray-500 text-xs font-bold uppercase tracking-wider block mb-1">Associated Invoice ID</span>
              <span className="font-semibold text-brand-orange break-all">{selectedTransactionDetails.invoiceId}</span>
            </div>
          )}
        </div>
        <div className="p-6 border-t border-gray-100 bg-gray-50 flex justify-end">
          <button onClick={() => setIsTransactionModalOpen(false)} className="px-6 py-2.5 bg-brand-orange hover:bg-orange-600 text-white rounded-full font-semibold transition-colors">
            Close
          </button>
        </div>
      </div>
    </div>
  )}
  {/* Analytics Drill-Down Details Modal */}
  {showAnalyticsDetailsModal && (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex justify-center items-center z-50 p-4 animate-fade-in" onClick={(e) => { if (e.target === e.currentTarget) setShowAnalyticsDetailsModal(false); }}>
      <div className="bg-white rounded-3xl w-full max-w-4xl shadow-2xl overflow-hidden flex flex-col transform transition-all">
        <div className="p-6 md:p-8 bg-gradient-to-r from-gray-50 to-white border-b border-gray-100 flex justify-between items-center relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-brand-orange/5 rounded-bl-full -mr-16 -mt-16 pointer-events-none"></div>
          <div>
            <h2 className="text-2xl font-bold text-gray-800">{analyticsDetailsTitle} Details</h2>
            <p className="text-gray-500 text-sm mt-1">Detailed list for your selected analytics criteria.</p>
          </div>
          <button onClick={() => setShowAnalyticsDetailsModal(false)} className="text-gray-400 hover:text-gray-600 bg-white hover:bg-gray-100 p-2 rounded-full transition-colors border border-gray-100 shadow-sm relative z-10">
            <X size={24} />
          </button>
        </div>
        
        <div className="p-6 md:p-8 space-y-6 flex-1 overflow-y-auto max-h-[70vh]">
          {isFetchingAnalyticsDetails ? (
            <div className="flex justify-center items-center h-48">
              <Loader2 className="animate-spin text-brand-orange w-8 h-8" />
            </div>
          ) : analyticsDetailsData.length === 0 ? (
            <div className="flex justify-center items-center h-48 bg-gray-50 rounded-2xl border border-gray-100">
              <span className="text-gray-500 font-medium">No records found.</span>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-gray-500 uppercase">
                    {analyticsDetailsType === 'equipment' ? (
                      <>
                        <th className="pb-3 font-semibold">Name</th>
                        <th className="pb-3 font-semibold">Model</th>
                        <th className="pb-3 font-semibold">Status</th>
                        <th className="pb-3 font-semibold">Price/Hr</th>
                      </>
                    ) : (
                      <>
                        <th className="pb-3 font-semibold">Equipment</th>
                        <th className="pb-3 font-semibold">User</th>
                        <th className="pb-3 font-semibold">Status</th>
                        <th className="pb-3 font-semibold">Amount</th>
                      </>
                    )}
                  </tr>
                </thead>
                <tbody>
                  {analyticsDetailsData.map(item => (
                    <tr key={item.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                      {analyticsDetailsType === 'equipment' ? (
                        <>
                          <td className="py-4 font-medium text-gray-800">{item.name}</td>
                          <td className="py-4 text-gray-600">{item.modelNumber}</td>
                          <td className="py-4">
                            <span className="px-3 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-700">
                              {item.status}
                            </span>
                          </td>
                          <td className="py-4 font-medium text-gray-900">₹{item.pricePerHour}</td>
                        </>
                      ) : (
                        <>
                          <td className="py-4 font-medium text-gray-800">{item.equipmentName}</td>
                          <td className="py-4 text-gray-600">{item.userName}</td>
                          <td className="py-4">
                            <span className="px-3 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-700">
                              {item.status}
                            </span>
                          </td>
                          <td className="py-4 font-medium text-gray-900">₹{item.totalAmount || 0}</td>
                        </>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Pagination Controls */}
        {!isFetchingAnalyticsDetails && analyticsDetailsTotalPages > 1 && (
          <div className="p-6 border-t border-gray-100 bg-gray-50 flex justify-between items-center">
            <button 
              disabled={analyticsDetailsPage === 0}
              onClick={() => fetchAnalyticsDetails(analyticsDetailsType, analyticsDetailsStatus, analyticsDetailsPage - 1)}
              className="px-5 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <span className="text-sm text-gray-600 font-medium">
              Page {analyticsDetailsPage + 1} of {analyticsDetailsTotalPages}
            </span>
            <button 
              disabled={analyticsDetailsPage >= analyticsDetailsTotalPages - 1}
              onClick={() => fetchAnalyticsDetails(analyticsDetailsType, analyticsDetailsStatus, analyticsDetailsPage + 1)}
              className="px-5 py-2 text-sm font-medium text-white bg-brand-orange border border-transparent rounded-lg hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )}

  {/* Dashboard KPI Drill-Down Modal */}
  {showKpiModal && (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex justify-center items-center z-50 p-4 animate-fade-in" onClick={(e) => { if (e.target === e.currentTarget) setShowKpiModal(false); }}>
      <div className="bg-white rounded-3xl w-full max-w-3xl shadow-2xl overflow-hidden flex flex-col transform transition-all">
        {/* Modal Header */}
        <div className="p-6 md:p-8 bg-gradient-to-r from-gray-50 to-white border-b border-gray-100 flex justify-between items-center relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-brand-orange/5 rounded-bl-full -mr-16 -mt-16 pointer-events-none"></div>
          <div>
            <h2 className="text-2xl font-bold text-gray-800">{kpiModalTitle}</h2>
            <p className="text-gray-500 text-sm mt-1">
              {kpiModalAuthorized
                ? `Showing ${kpiModalData.length} record${kpiModalData.length !== 1 ? 's' : ''}`
                : 'You can view summary data. Full details require a management role.'}
            </p>
          </div>
          <button onClick={() => setShowKpiModal(false)} className="text-gray-400 hover:text-gray-600 bg-white hover:bg-gray-100 p-2 rounded-full transition-colors border border-gray-100 shadow-sm relative z-10">
            <X size={24} />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 md:p-8 overflow-y-auto max-h-[65vh]">
          {!kpiModalAuthorized ? (
            /* Unauthorized View - show summary only */
            <div className="flex flex-col items-center gap-6 py-8">
              <div className="w-20 h-20 bg-amber-50 rounded-full flex items-center justify-center">
                <Shield size={36} className="text-amber-500" />
              </div>
              <div className="text-center">
                <h3 className="text-xl font-bold text-gray-800 mb-2">Summary View</h3>
                <p className="text-gray-500 max-w-sm">You can see the count but detailed records are only available to Lab Managers, Dept Heads, and Admins.</p>
              </div>
              <div className="bg-gray-50 rounded-2xl px-12 py-8 border border-gray-100 text-center">
                <div className="text-5xl font-black text-gray-800 mb-2">{kpiModalData.length}</div>
                <div className="text-gray-500 font-medium">{kpiModalTitle}</div>
              </div>
            </div>
          ) : kpiModalData.length === 0 ? (
            <div className="flex justify-center items-center h-40 bg-gray-50 rounded-2xl border border-gray-100">
              <span className="text-gray-500 font-medium">No records found.</span>
            </div>
          ) : (
            <div className="overflow-x-auto">
              {/* Equipment Table */}
              {(kpiModalType === 'equipment' || kpiModalType === 'maintenance') && (
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 text-gray-500 uppercase text-xs tracking-wider">
                      <th className="pb-3 font-semibold">Equipment Name</th>
                      <th className="pb-3 font-semibold">Model</th>
                      <th className="pb-3 font-semibold">Status</th>
                      <th className="pb-3 font-semibold text-right">Price/Hr</th>
                    </tr>
                  </thead>
                  <tbody>
                    {kpiModalData.map(item => (
                      <tr key={item.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                        <td className="py-4 font-medium text-gray-800">{item.name}</td>
                        <td className="py-4 text-gray-500">{item.modelNumber || 'â€”'}</td>
                        <td className="py-4">
                          <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                            item.status === 'AVAILABLE' ? 'bg-green-100 text-green-700' :
                            item.status === 'UNDER_MAINTENANCE' ? 'bg-red-100 text-red-700' :
                            item.status === 'BOOKED' ? 'bg-blue-100 text-blue-700' :
                            'bg-gray-100 text-gray-700'
                          }`}>{item.status}</span>
                        </td>
                        <td className="py-4 font-semibold text-gray-900 text-right">₹{item.pricePerHour || 0}/hr</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {/* Pending Approvals Table with Approve/Reject Actions */}
              {kpiModalType === 'pending' && (
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 text-gray-500 uppercase text-xs tracking-wider">
                      <th className="pb-3 font-semibold">User</th>
                      <th className="pb-3 font-semibold">Equipment</th>
                      <th className="pb-3 font-semibold">Time Slot</th>
                      <th className="pb-3 font-semibold text-center">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {kpiModalData.map(item => (
                      <tr key={item.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                        <td className="py-4 font-medium text-gray-800">{item.userName || 'â€”'}</td>
                        <td className="py-4 text-gray-600">{item.equipmentName || 'â€”'}</td>
                        <td className="py-4 text-gray-500 text-xs">
                          {item.startTime ? new Date(item.startTime).toLocaleString() : 'â€”'}
                          {item.endTime ? ` → ${new Date(item.endTime).toLocaleTimeString()}` : ''}
                        </td>
                        <td className="py-4">
                          <div className="flex items-center justify-center gap-2">
                            <button
                              onClick={async () => {
                                try {
                                  await api.patch(`/bookings/${item.id}/status?status=CONFIRMED`);
                                  setKpiModalData(prev => prev.filter(b => b.id !== item.id));
                                  fetchBookings();
                                } catch(e) { alert('Failed to approve: ' + (e.response?.data?.message || e.message)); }
                              }}
                              className="flex items-center gap-1.5 px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-xl text-xs font-bold transition-all shadow-sm hover:shadow-md"
                            >
                              <CheckCircle size={14} /> Approve
                            </button>
                            <button
                              onClick={async () => {
                                if (!window.confirm('Are you sure you want to reject this booking?')) return;
                                try {
                                  await api.patch(`/bookings/${item.id}/status?status=CANCELLED`);
                                  setKpiModalData(prev => prev.filter(b => b.id !== item.id));
                                  fetchBookings();
                                } catch(e) { alert('Failed to reject: ' + (e.response?.data?.message || e.message)); }
                              }}
                              className="flex items-center gap-1.5 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-xl text-xs font-bold transition-all shadow-sm hover:shadow-md"
                            >
                              <X size={14} /> Reject
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {/* Bookings Table (Today's Bookings) */}
              {kpiModalType === 'bookings' && (
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 text-gray-500 uppercase text-xs tracking-wider">
                      <th className="pb-3 font-semibold">User</th>
                      <th className="pb-3 font-semibold">Equipment</th>
                      <th className="pb-3 font-semibold">Time Slot</th>
                      <th className="pb-3 font-semibold">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {kpiModalData.map(item => (
                      <tr key={item.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                        <td className="py-4 font-medium text-gray-800">{item.userName || 'â€”'}</td>
                        <td className="py-4 text-gray-600">{item.equipmentName || 'â€”'}</td>
                        <td className="py-4 text-gray-500 text-xs">
                          {item.startTime ? new Date(item.startTime).toLocaleString() : 'â€”'}
                          {item.endTime ? ` → ${new Date(item.endTime).toLocaleTimeString()}` : ''}
                        </td>
                        <td className="py-4">
                          <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                            item.status === 'CONFIRMED' ? 'bg-green-100 text-green-700' :
                            item.status === 'PENDING' ? 'bg-amber-100 text-amber-700' :
                            item.status === 'COMPLETED' ? 'bg-blue-100 text-blue-700' :
                            item.status === 'CANCELLED' ? 'bg-red-100 text-red-700' :
                            'bg-gray-100 text-gray-700'
                          }`}>{item.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-gray-100 bg-gray-50 flex justify-end">
          <button onClick={() => setShowKpiModal(false)} className="px-6 py-2 text-sm font-semibold text-gray-600 bg-white border border-gray-200 rounded-xl hover:bg-gray-100 transition-colors">
            Close
          </button>
        </div>
      </div>
    </div>
  )}

  </div>
  </ErrorBoundary>
  );
};

export default Dashboard;

// ---------------------------------------------------------------------------------- 
// EnterpriseInvoiceBody — Brand-matched, A4 print-optimised invoice layout
// Uses the LabResource brand orange (#ff4500) as accent throughout.
// ---------------------------------------------------------------------------------- 
function EnterpriseInvoiceBody({ inv }) {
  if (!inv) return null;

  // INR currency formatter for all BigDecimal values from the backend
  const fmt = (v) => new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR',
    minimumFractionDigits: 2, maximumFractionDigits: 2,
  }).format(parseFloat(v ?? 0));

  const fmtDate = (d, opts = {day:'2-digit', month:'short', year:'numeric'}) =>
    d ? new Date(d).toLocaleDateString('en-IN', opts) : '—';

  // Status badge styles
  const statusStyle = {
    PAID:             { bg: '#ecfdf5', border: '#6ee7b7', color: '#047857', label: '✓ PAID' },
    ISSUED:           { bg: '#eff6ff', border: '#93c5fd', color: '#1d4ed8', label: 'ISSUED' },
    APPROVED:         { bg: '#eef2ff', border: '#a5b4fc', color: '#4338ca', label: 'APPROVED' },
    PENDING_APPROVAL: { bg: '#fffbeb', border: '#fcd34d', color: '#92400e', label: 'PENDING' },
    DRAFT:            { bg: '#f8fafc', border: '#cbd5e1', color: '#475569', label: 'DRAFT' },
    OVERDUE:          { bg: '#fef2f2', border: '#fca5a5', color: '#b91c1c', label: 'OVERDUE' },
  };
  const ss = statusStyle[inv.status] ?? statusStyle.DRAFT;

  const subtotal   = parseFloat(inv.subtotalAmount ?? inv.totalAmount ?? 0);
  const taxAmount  = parseFloat(inv.taxAmount ?? 0);
  const grandTotal = parseFloat(inv.totalAmount ?? 0);
  const overheadPct = inv.overheadRate ? (parseFloat(inv.overheadRate) * 100).toFixed(0) : '0';
  const v = inv.vendor ?? {};

  return (
    <div className="enterprise-invoice-body bg-white font-sans" style={{color:'#1e293b'}}>

      {/* â”€â”€ BRAND HEADER STRIP â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      {/* -- BRAND HEADER STRIP -- */}
      <div style={{background:'linear-gradient(135deg, #fff3e8 0%, #ffe8d2 100%)', padding:'20px 40px 18px', borderBottom:'2px solid #fdd5b0'}}>
        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}>
          {/* Brand */}
          <div>
            <div style={{display:'flex', alignItems:'center', gap:'8px', marginBottom:'2px'}}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#c2410c" strokeWidth="2.5">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
              </svg>
              <span style={{fontSize:'22px', fontWeight:'900', color:'#9a3412', letterSpacing:'-0.5px'}}>LabResource</span>
            </div>
            <p style={{fontSize:'10px', color:'#c2410c', textTransform:'uppercase', letterSpacing:'1.5px'}}>
              Central Equipment Booking System
            </p>
          </div>
          {/* INVOICE label */}
          <div style={{textAlign:'right'}}>
            <p style={{fontSize:'12px', fontWeight:'700', color:'#9a3412', letterSpacing:'5px', textTransform:'uppercase', lineHeight:'1', padding:'5px 12px', background:'rgba(194,65,12,0.1)', border:'1px solid #fdd5b0', borderRadius:'6px', display:'inline-block'}}>INVOICE</p>
            <p style={{fontSize:'11px', fontFamily:'monospace', color:'#c2410c', marginTop:'4px'}}>
              {inv.id ? `#INV-${inv.id.substring(0,8).toUpperCase()}` : '#DRAFT'}
            </p>
            {inv.billingPeriodStart && inv.billingPeriodEnd && (
              <p style={{fontSize:'9px', color:'#c2410c', marginTop:'2px', opacity:0.75}}>
                Period: {fmtDate(inv.billingPeriodStart)} - {fmtDate(inv.billingPeriodEnd)}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* â”€â”€ BODY CONTENT â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      <div style={{padding:'28px 40px 32px'}}>

        {/* â”€â”€ SECTION 2: Address + Metadata â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
        <div style={{display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:'16px', marginBottom:'28px'}}>

          {/* Remit To */}
          <div>
            <p style={{fontSize:'9px', fontWeight:'700', textTransform:'uppercase', letterSpacing:'2px', color:'#ff4500', marginBottom:'6px'}}>
              Remit To
            </p>
            <div style={{border:'1px solid #ffe4d9', borderRadius:'10px', padding:'12px 14px', background:'#fff8f6'}}>
              <p style={{fontWeight:'700', fontSize:'13px', color:'#1e293b'}}>{v.platformName ?? 'LabResource'}</p>
              <p style={{fontSize:'10px', color:'#64748b', marginTop:'2px'}}>{v.legalEntity ?? 'SBM TechServices Pvt. Ltd.'}</p>
              <p style={{fontSize:'10px', color:'#94a3b8', marginTop:'8px'}}>{v.addressLine1 ?? 'Block A, Research Park, Sector 125'}</p>
              <p style={{fontSize:'10px', color:'#94a3b8'}}>{v.addressLine2 ?? 'Noida, UP – 201303, India'}</p>
              <p style={{fontSize:'10px', color:'#94a3b8', marginTop:'6px'}}>
                <span style={{color:'#64748b', fontWeight:'600'}}>Tax ID:</span> {v.taxId ?? 'GSTIN: 09AABCS1234Q1ZX'}
              </p>
              <p style={{fontSize:'10px', color:'#94a3b8'}}>{v.website ?? 'www.labresource.edu'}</p>
            </div>
          </div>

          {/* Billed To */}
          <div>
            <p style={{fontSize:'9px', fontWeight:'700', textTransform:'uppercase', letterSpacing:'2px', color:'#ff4500', marginBottom:'6px'}}>
              Billed To
            </p>
            <div style={{border:'1px solid #e2e8f0', borderRadius:'10px', padding:'12px 14px', background:'#f8fafc'}}>
              <p style={{fontWeight:'700', fontSize:'13px', color:'#1e293b'}}>
                {inv.billedToInstitutionName || inv.billedToDepartmentName || 'Client'}
              </p>
              {inv.billedToDepartmentName && inv.billedToInstitutionName && (
                <p style={{fontSize:'10px', color:'#64748b', marginTop:'2px'}}>{inv.billedToDepartmentName}</p>
              )}
              <p style={{fontSize:'10px', color:'#94a3b8', marginTop:'8px'}}>India</p>
            </div>
          </div>

          {/* Invoice Details */}
          <div>
            <p style={{fontSize:'9px', fontWeight:'700', textTransform:'uppercase', letterSpacing:'2px', color:'#ff4500', marginBottom:'6px'}}>
              Invoice Details
            </p>
            <div style={{border:'1px solid #e2e8f0', borderRadius:'10px', padding:'12px 14px', background:'#f8fafc'}}>
              {/* Status badge */}
              <span style={{
                display:'inline-block', padding:'3px 10px', borderRadius:'999px', fontSize:'10px',
                fontWeight:'700', textTransform:'uppercase', letterSpacing:'0.8px', marginBottom:'10px',
                background: ss.bg, border:`1px solid ${ss.border}`, color: ss.color
              }}>{inv.status === 'PAID' ? 'PAID' : ss.label.replace('✓ ','')}</span>
              <table style={{width:'100%', fontSize:'10px', borderCollapse:'collapse'}}>
                <tbody>
                  {[
                    ['Invoice #', inv.id ? `INV-${inv.id.substring(0,8).toUpperCase()}` : 'â€”'],
                    ['Issue Date', fmtDate(inv.invoiceDate)],
                    ['Due Date',   fmtDate(inv.dueDate)],
                    ...(inv.approvedByName ? [['Approved By', inv.approvedByName]] : []),
                  ].map(([label, val], i) => (
                    <tr key={i} style={{borderBottom:'1px solid #f1f5f9'}}>
                      <td style={{padding:'4px 0', color:'#94a3b8', fontWeight:'600'}}>{label}</td>
                      <td style={{padding:'4px 0', textAlign:'right', color:'#1e293b', fontWeight:'600', fontFamily:'monospace'}}>{val}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* â”€â”€ SECTION 3: Line Items Table â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
        <div style={{border:'1px solid #fee2d0', borderRadius:'12px', overflow:'hidden', marginBottom:'24px'}}>
          <table style={{width:'100%', borderCollapse:'collapse', fontSize:'12px'}}>
            <thead>
              <tr style={{background:'linear-gradient(90deg, #fff3ee 0%, #fff8f5 100%)'}}>
                <th style={{textAlign:'left', padding:'11px 18px', fontSize:'10px', fontWeight:'700', textTransform:'uppercase', letterSpacing:'1.5px', color:'#ff4500', width:'40%'}}>Description</th>
                <th style={{textAlign:'center', padding:'11px 12px', fontSize:'10px', fontWeight:'700', textTransform:'uppercase', letterSpacing:'1.5px', color:'#c2410c'}}>Provider</th>
                <th style={{textAlign:'right', padding:'11px 12px', fontSize:'10px', fontWeight:'700', textTransform:'uppercase', letterSpacing:'1.5px', color:'#c2410c'}}>Qty / Hrs</th>
                <th style={{textAlign:'right', padding:'11px 12px', fontSize:'10px', fontWeight:'700', textTransform:'uppercase', letterSpacing:'1.5px', color:'#c2410c'}}>Rate</th>
                <th style={{textAlign:'right', padding:'11px 18px', fontSize:'10px', fontWeight:'700', textTransform:'uppercase', letterSpacing:'1.5px', color:'#ff4500'}}>Amount</th>
              </tr>
            </thead>
            <tbody>
              {(inv.lineItems ?? []).length === 0 ? (
                <tr><td colSpan={5} style={{padding:'32px', textAlign:'center', color:'#94a3b8'}}>No line items found.</td></tr>
              ) : (inv.lineItems ?? []).map((item, i) => (
                <tr key={i} className="invoice-row" style={{borderTop:'1px solid #fff0eb', background: i % 2 === 0 ? 'white' : '#fffcfb'}}>
                  <td style={{padding:'13px 18px'}}>
                    <p style={{fontWeight:'600', color:'#1e293b', marginBottom:'2px'}}>
                      {item.equipmentName || item.description || 'Equipment Usage'}
                    </p>
                    {item.bookingStartTime && item.bookingEndTime && (
                      <p style={{fontSize:'10px', color:'#94a3b8'}}>
                        {new Date(item.bookingStartTime).toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'})}
                        {' '}–{' '}
                        {new Date(item.bookingEndTime).toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'})}
                      </p>
                    )}
                    {item.referenceId && (
                      <p style={{fontSize:'9px', color:'#cbd5e1', fontFamily:'monospace', marginTop:'1px'}}>Ref: {item.referenceId}</p>
                    )}
                  </td>
                  <td style={{padding:'13px 12px', textAlign:'center', color:'#64748b', fontSize:'11px'}}>
                    {item.equipmentInstituteName ?? 'â€”'}
                  </td>
                  <td style={{padding:'13px 12px', textAlign:'right', fontFamily:'monospace', color:'#334155'}}>
                    {parseFloat(item.quantity ?? 1).toFixed(1)}
                  </td>
                  <td style={{padding:'13px 12px', textAlign:'right', fontFamily:'monospace', color:'#334155'}}>
                    {fmt(item.unitPrice)}
                  </td>
                  <td style={{padding:'13px 18px', textAlign:'right', fontFamily:'monospace', fontWeight:'700', color:'#1e293b'}}>
                    {fmt(item.lineTotal)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* â”€â”€ SECTION 4: Financial Summary â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
        <div style={{display:'flex', justifyContent:'flex-end', marginBottom:'24px'}}>
          <div style={{width:'300px'}}>
            <div style={{display:'flex', justifyContent:'space-between', padding:'7px 0', fontSize:'13px', color:'#64748b', borderBottom:'1px solid #f1f5f9'}}>
              <span>Subtotal</span>
              <span style={{fontFamily:'monospace', fontWeight:'500'}}>{fmt(subtotal)}</span>
            </div>
            <div style={{display:'flex', justifyContent:'space-between', padding:'7px 0', fontSize:'13px', color:'#64748b', borderBottom:'1px solid #f1f5f9'}}>
              <span>{taxAmount > 0 ? `Overhead (${overheadPct}%)` : 'Tax / Overhead'}</span>
              <span style={{fontFamily:'monospace', fontWeight:'500'}}>{fmt(taxAmount)}</span>
            </div>
            {/* Grand Total highlight strip */}
            <div style={{
              marginTop:'10px', borderRadius:'10px', padding:'14px 18px',
              background:'linear-gradient(135deg, #ff4500 0%, #ff6a33 100%)',
              display:'flex', justifyContent:'space-between', alignItems:'center'
            }}>
              <span style={{fontWeight:'700', color:'white', fontSize:'14px'}}>Grand Total</span>
              <span style={{fontFamily:'monospace', fontWeight:'900', color:'white', fontSize:'22px'}}>{fmt(grandTotal)}</span>
            </div>
            {/* Status tag below total */}
            {inv.status === 'PAID' && (
              <div style={{marginTop:'8px', textAlign:'center', padding:'6px', borderRadius:'8px', background:'#ecfdf5', border:'1px solid #6ee7b7'}}>
                <span style={{color:'#047857', fontWeight:'700', fontSize:'12px', letterSpacing:'0.5px'}}>Paid in Full</span>
              </div>
            )}
            {(inv.status === 'ISSUED' || inv.status === 'PENDING_APPROVAL') && (
              <div style={{marginTop:'8px', textAlign:'center', padding:'6px', borderRadius:'8px', background:'#fffbeb', border:'1px solid #fcd34d'}}>
                <span style={{color:'#92400e', fontWeight:'600', fontSize:'11px'}}>Payment due within Net 30 days</span>
              </div>
            )}
          </div>
        </div>

        {/* -- SECTION 5: Notes -- */}
        {inv.notes && (
          <div style={{marginBottom:'20px', padding:'12px 16px', borderRadius:'10px', background:'#fff8f6', border:'1px solid #ffe4d9'}}>
            <p style={{fontSize:'9px', fontWeight:'700', color:'#ff4500', textTransform:'uppercase', letterSpacing:'1.5px', marginBottom:'4px'}}>Notes</p>
            <p style={{fontSize:'12px', color:'#475569'}}>{inv.notes}</p>
          </div>
        )}

        {/* -- SECTION 6: Auth Seal + Footer -- */}
        <div style={{borderTop:'2px solid #fff0eb', paddingTop:'18px', marginTop:'4px'}}>

          {/* Digital auth banner */}
          <div style={{
            display:'flex', alignItems:'flex-start', gap:'12px', padding:'12px 16px',
            borderRadius:'10px', background:'#f8fafc', border:'1px solid #e2e8f0', marginBottom:'16px'
          }}>
            <div style={{
              width:'36px', height:'36px', borderRadius:'50%', flexShrink:0,
              background:'#ecfdf5', border:'2px solid #6ee7b7', display:'flex', alignItems:'center', justifyContent:'center'
            }}>
              <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="#047857" strokeWidth="2.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.955 11.955 0 01.458 10c.595 4.624 3.487 8.527 7.542 10 4.055-1.473 6.947-5.376 7.542-10A11.955 11.955 0 0118.402 6a11.959 11.959 0 01-2.29-3.286M15 9.75a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <div style={{flex:1}}>
              <p style={{fontSize:'10px', fontWeight:'700', color:'#047857', textTransform:'uppercase', letterSpacing:'1px', marginBottom:'3px'}}>
                Auto-Generated &amp; Digitally Authenticated
              </p>
              <p style={{fontSize:'9px', color:'#64748b', lineHeight:'1.5'}}>
                System-generated by <strong>LabResource CEBS</strong>. Legally valid under the Information Technology Act, 2000 (India). No manual signature required.
              </p>
              <div style={{marginTop:'5px', display:'flex', flexWrap:'wrap', gap:'0 16px', fontSize:'8px', fontFamily:'monospace', color:'#94a3b8'}}>
                <span><span style={{color:'#64748b', fontWeight:'600'}}>DOC-ID:</span> {inv.id ? inv.id.toUpperCase() : 'N/A'}</span>
                <span><span style={{color:'#64748b', fontWeight:'600'}}>GEN-AT:</span> {new Date().toISOString().replace('T',' ').substring(0,19)} UTC</span>
                <span><span style={{color:'#64748b', fontWeight:'600'}}>SYS:</span> LRCEBS-v2.0</span>
              </div>
            </div>
          </div>

          {/* Payment terms + contact */}
          <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'24px', fontSize:'10px', color:'#94a3b8'}}>
            <div>
              <p style={{fontWeight:'700', color:'#64748b', marginBottom:'3px'}}>Payment Terms</p>
              <p>Net 30 days from issue date. Late payments attract interest at 1.5% / month.</p>
              <p style={{marginTop:'3px'}}>
                Quote Invoice{' '}
                <span style={{fontFamily:'monospace', fontWeight:'700', color:'#ff4500'}}>
                  {inv.id ? `INV-${inv.id.substring(0,8).toUpperCase()}` : ''}
                </span>{' '}
                in all remittances.
              </p>
            </div>
            <div style={{textAlign:'right'}}>
              <p style={{fontWeight:'700', color:'#64748b', marginBottom:'3px'}}>
                LabResource - {v.legalEntity ?? 'SBM TechServices Pvt. Ltd.'}
              </p>
              <p>{v.email ?? 'billing@labresource.edu'} . {v.phone ?? '+91-120-4567-890'}</p>
              <p>{v.website ?? 'www.labresource.edu'}</p>
              <p style={{marginTop:'4px', fontSize:'8px', color:'#cbd5e1'}}>
                Auto-generated - {new Date().toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'})}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

