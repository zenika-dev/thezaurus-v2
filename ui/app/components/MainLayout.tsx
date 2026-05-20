import React from 'react';
import { Box, styled } from '@mui/material';
import SideMenu from './SideMenu';

const LayoutRoot = styled(Box)({
  display: 'flex',
  minHeight: '100vh',
  width: '100%',
  backgroundColor: '#fff',
});

const ContentWrapper = styled(Box)({
  flexGrow: 1,
  display: 'flex',
  flexDirection: 'column',
  minWidth: 0,
  padding: '24px',
});

interface MainLayoutProps {
  children: React.ReactNode;
}

export const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
  return (
    <LayoutRoot>
      <SideMenu />
      <ContentWrapper>
        {children}
      </ContentWrapper>
    </LayoutRoot>
  );
};

export default MainLayout;
