import { useState } from 'react';
import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  IconButton,
  Typography,
  useTheme,
  styled,
  Tooltip,
  Fade,
} from '@mui/material';
import { BookOpen, Calendar, CalendarDays, MicVocal, PenLine } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router';

const DRAWER_WIDTH = 255;
const COLLAPSED_WIDTH = 88;

const StyledDrawer = styled(Drawer, { shouldForwardProp: (prop) => prop !== 'open' })(
  ({ theme, open }) => ({
    width: open ? DRAWER_WIDTH : COLLAPSED_WIDTH,
    flexShrink: 0,
    whiteSpace: 'nowrap',
    boxSizing: 'border-box',
    '& .MuiDrawer-paper': {
      width: open ? DRAWER_WIDTH : COLLAPSED_WIDTH,
      transition: theme.transitions.create(['width', 'background-color'], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.enteringScreen,
      }),
      overflow: 'visible',
      borderRight: '1px solid rgba(0, 0, 0, 0.08)',
      background: '#ffffff',
      boxShadow: 'none',
    },
  }),
);

const LogoContainer = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  padding: theme.spacing(4, 3),
  height: 100,
  justifyContent: 'flex-start',
}));

const NavItem = ({ icon: Icon, label, open, active = false, onClick }: any) => {
  const theme = useTheme();
  
  const content = (
    <ListItem disablePadding sx={{ display: 'block', mb: 0.4, px: 1.5 }}>
      <ListItemButton
        onClick={onClick}
        sx={{
          minHeight: 44,
          display: 'flex',
          alignItems: 'center',
          justifyContent: open ? 'initial' : 'center',
          px: 2,
          borderRadius: 1,
          position: 'relative',
          backgroundColor: active ? 'rgba(237, 33, 60, 0.08)' : 'transparent',
          color: active ? theme.palette.primary.main : '#4d4d4d',
          '&:hover': {
            backgroundColor: active ? 'rgba(237, 33, 60, 0.12)' : 'rgba(237, 33, 60, 0.04)',
            color: theme.palette.primary.main,
          },
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        }}
      >

        <ListItemIcon
          sx={{
            minWidth: 0,
            mr: open ? 2 : 0,
            justifyContent: 'center',
            display: 'flex',
            alignItems: 'center',
            color: active ? theme.palette.primary.main : 'inherit',
          }}
        >
          <Icon size={22} />
        </ListItemIcon>
        {open && (
          <Fade in={open} timeout={400}>
            <ListItemText 
              primary={label} 
              sx={{ 
                m: 0,
                display: 'flex',
                alignItems: 'center',
                '& .MuiTypography-root': {
                  fontSize: '0.9rem',
                  lineHeight: 1,
                }
              }} 
            />
          </Fade>
        )}
      </ListItemButton>
    </ListItem>
  );

  return open ? content : (
    <Tooltip title={label} placement="right" arrow>
      {content}
    </Tooltip>
  );
};

const navItems = [
  { icon: Calendar, label: 'Événements', path: '/' },
  { icon: MicVocal, label: 'Talks', path: '/talks' },
  { icon: CalendarDays, label: 'Conférences', path: '/conferences' },
  { icon: PenLine, label: 'Blog Posts', path: '/blog-posts' },
];

export const SideMenu = () => {
  const [open, setOpen] = useState(true);
  const theme = useTheme();
  const navigate = useNavigate();
  const location = useLocation();

  const toggleDrawer = () => {
    setOpen(!open);
  };

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  return (
    <StyledDrawer variant="permanent" open={open}>
      <LogoContainer sx={{ 
        justifyContent: open ? 'flex-start' : 'center', 
        px: open ? 2.5 : 0,
        height: 64,
        py: 0,
        position: 'relative',
        borderBottom: '1px solid rgba(0, 0, 0, 0.08)',
      }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <Box 
            sx={{ 
              width: 36, 
              height: 36, 
              borderRadius: '12px', 
              backgroundColor: '#D51F51',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'white',
              fontWeight: 900,
              fontSize: '1.2rem',
              boxShadow: theme.shadows[1],
              flexShrink: 0,
            }}
          >
            <BookOpen size={20} />
          </Box>
          {open && (
            <Fade in={open} timeout={600}>
              <Box sx={{ ml: 2, display: 'flex', flexDirection: 'column' }}>
                <Typography 
                  sx={{ 
                    fontWeight: 800, 
                    color: '#1e293b',
                    letterSpacing: '-0.5px',
                    fontSize: '1.125rem',
                    lineHeight: 1.2
                  }}
                >
                  Thezaurus
                </Typography>
                <Typography 
                  sx={{ 
                    fontSize: '10px', 
                    textTransform: 'uppercase', 
                    letterSpacing: '0.1em', 
                    color: '#737373',
                    fontWeight: 700,
                    mt: 0.5
                  }}
                >
                  by Zenika
                </Typography>
              </Box>
            </Fade>
          )}
        </Box>
        
        <IconButton 
          onClick={toggleDrawer}
          sx={{
            width: 28,
            height: 28,
            backgroundColor: open ? 'rgba(0, 0, 0, 0.04)' : 'white',
            borderRadius: '50%',
            color: '#64748b',
            position: 'absolute',
            right: open ? 20 : -14,
            top: '50%',
            transform: 'translateY(-50%)',
            zIndex: 10,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: open ? 'none' : '0 2px 8px rgba(0,0,0,0.08)',
            border: open ? 'none' : '1px solid rgba(0, 0, 0, 0.08)',
            '&:hover': { 
              backgroundColor: '#D51F51',
              color: 'white',
              transform: 'translateY(-50%) scale(1.1)',
              boxShadow: open ? 'none' : `0 4px 12px ${theme.palette.primary.main}40`,
            },
            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            fontSize: '1.2rem',
            lineHeight: 1,
            p: 0,
          }}
        >
          {open ? '‹' : '›'}
        </IconButton>
      </LogoContainer>

      <Box sx={{ flexGrow: 1, overflowY: 'auto', overflowX: 'hidden', pt: 1 }}>
        <List>
        {navItems.map((item) => (
          <NavItem 
            key={item.label} 
            icon={item.icon} 
            label={item.label} 
            open={open} 
            active={isActive(item.path)}
            onClick={() => navigate(item.path)}
          />
        ))}
      </List>
      </Box>
    </StyledDrawer>
  );
};

export default SideMenu;
