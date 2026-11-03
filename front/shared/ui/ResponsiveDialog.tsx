"use client";

import Dialog, { type DialogProps } from "@mui/material/Dialog";
import useMediaQuery from "@mui/material/useMediaQuery";
import { useEffect, useState } from "react";

/** Keep MUI's focus trap, return-focus and escape handling on every screen. */
export function ResponsiveDialog({ fullScreen, sx, ...props }: DialogProps) {
  const mobile = useMediaQuery("(max-width: 599px)");
  const isFullScreen = fullScreen ?? mobile;
  const [viewport, setViewport] = useState<{ height: number; top: number } | null>(null);

  useEffect(() => {
    const visualViewport = window.visualViewport;
    if (!props.open || !isFullScreen || !visualViewport) return;
    // Some mobile browsers shrink only the visual viewport when the keyboard opens.
    // Leave pinch zoom to the browser instead of resizing the form while zooming.
    const update = () => setViewport(visualViewport.scale === 1
      ? { height: visualViewport.height, top: visualViewport.offsetTop }
      : null);
    const frame = requestAnimationFrame(update);
    visualViewport.addEventListener("resize", update);
    visualViewport.addEventListener("scroll", update);
    return () => {
      cancelAnimationFrame(frame);
      visualViewport.removeEventListener("resize", update);
      visualViewport.removeEventListener("scroll", update);
    };
  }, [props.open, isFullScreen]);

  return <Dialog {...props} fullScreen={isFullScreen} sx={[
    ...(isFullScreen && viewport ? [{
      "& .MuiDialog-container": { height: `${viewport.height}px`, marginTop: `${viewport.top}px` },
      "& .MuiDialog-paper.MuiDialog-paperFullScreen": { height: "100%", maxHeight: "100%" },
    }] : []),
    ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
  ]} />;
}
