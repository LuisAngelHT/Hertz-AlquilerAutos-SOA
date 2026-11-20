"use client";

import { useEffect, useState, type ReactNode } from "react";

interface AutoDismissFeedbackProps {
  role: "alert" | "status";
  className: string;
  children: ReactNode;
}

const DURACION_MS = 5000;

export function AutoDismissFeedback({
  role,
  className,
  children,
}: AutoDismissFeedbackProps) {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const timer = window.setTimeout(() => setVisible(false), DURACION_MS);
    return () => window.clearTimeout(timer);
  }, []);

  if (!visible) return null;

  return (
    <div role={role} aria-atomic="true" className={className}>
      {children}
    </div>
  );
}
