import React from 'react';
import { Box, Typography } from '@mui/material';

const BlogPosts: React.FC = () => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '60vh',
        gap: 2,
      }}
    >
      <Typography
        variant="h4"
        sx={{
          fontWeight: 800,
          color: '#1e293b',
          letterSpacing: '-0.5px',
        }}
      >
        Blog Posts
      </Typography>
      <Typography
        variant="body1"
        sx={{
          color: '#94a3b8',
          fontSize: '1.1rem',
        }}
      >
        Cette section est en cours de construction.
      </Typography>
    </Box>
  );
};

export default BlogPosts;
