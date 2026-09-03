import type { HTMLAttributes } from 'react';
import { cn } from '../lib/cn';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  title?: string;
}

export function Card({ title, className, children, ...props }: CardProps) {
  return (
    <div
      className={cn('rounded-xl border border-border bg-surface p-6 shadow-card', className)}
      {...props}
    >
      {title && <h2 className="mb-4 font-semibold">{title}</h2>}
      {children}
    </div>
  );
}
