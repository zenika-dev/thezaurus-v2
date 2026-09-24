FROM node:24-alpine AS deps
WORKDIR /app
COPY front/package.json front/package-lock.json ./
RUN npm ci --ignore-scripts

FROM deps AS dev
ENV NEXT_TELEMETRY_DISABLED=1
COPY front/ .
COPY api/openapi.json /api/openapi.json
RUN npm run generate:api
EXPOSE 3000
CMD ["npm", "run", "dev", "--", "-H", "0.0.0.0", "-p", "3000"]

FROM node:24-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY front/ .
COPY api/openapi.json /api/openapi.json
ARG GOOGLE_CLIENT_ID
ENV GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
RUN npm run build

FROM node:24-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
CMD ["node", "server.js"]
