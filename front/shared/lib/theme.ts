import { createTheme, type Shadows } from "@mui/material/styles";

const theme = createTheme({
  palette: {
    primary: { main: "#ed213c" },
  },
  typography: {
    fontFamily: '"Nunito", "Roboto", "Helvetica", "Arial", sans-serif',
  },
  shape: { borderRadius: 16 },
  shadows: [
    "none",
    "2px 2px 3px rgba(0, 0, 0, .35)",
    ...createTheme().shadows.slice(2),
  ] as Shadows,
  components: {
    MuiButton: {
      styleOverrides: {
        root: { textTransform: "none" },
        contained: {
          color: "#FFFFFF",
          boxShadow: "2px 2px 3px rgba(0, 0, 0, .35)",
          "&:hover": { boxShadow: "2px 2px 4px rgba(0, 0, 0, .4)" },
        },
        outlined: {
          color: "#000000",
          "&:hover": { backgroundColor: "rgba(237, 33, 60, 0.04)" },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { boxShadow: "0px 2px 4px rgba(0, 0, 0, 0.15)" },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          "&.Mui-focused .MuiOutlinedInput-notchedOutline": { borderColor: "#ed213c" },
          "&:hover .MuiOutlinedInput-notchedOutline": { borderColor: "#ed213c" },
        },
      },
    },
    MuiInputLabel: {
      styleOverrides: {
        root: { "&.Mui-focused": { color: "#ed213c" } },
      },
    },
  },
});

export default theme;
