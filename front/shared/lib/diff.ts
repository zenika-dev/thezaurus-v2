export interface DiffToken {
  type: "unchanged" | "added" | "removed";
  value: string;
}

/**
  * Tokenizes a text into words, whitespace, and punctuation.
  */
function tokenize(text: string): string[] {
  if (!text) return [];
  // Match word characters or non-word characters (whitespace, punctuation)
  const matches = text.match(/\S+|\s+/g);
  return matches || [];
}

/**
  * Computes word-level diff between original and suggested strings using LCS algorithm.
  */
export function computeWordDiff(original: string, suggested: string): {
  originalTokens: DiffToken[];
  suggestedTokens: DiffToken[];
  diffTokens: DiffToken[];
} {
  const origTokens = tokenize(original);
  const suggTokens = tokenize(suggested);

  const n = origTokens.length;
  const m = suggTokens.length;

  // Build LCS DP matrix
  const dp: number[][] = Array.from({ length: n + 1 }, () => Array(m + 1).fill(0));

  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      if (origTokens[i] === suggTokens[j]) {
        dp[i][j] = 1 + dp[i + 1][j + 1];
      } else {
        dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
      }
    }
  }

  const originalTokens: DiffToken[] = [];
  const suggestedTokens: DiffToken[] = [];
  const diffTokens: DiffToken[] = [];

  let i = 0;
  let j = 0;

  while (i < n && j < m) {
    if (origTokens[i] === suggTokens[j]) {
      const val = origTokens[i];
      originalTokens.push({ type: "unchanged", value: val });
      suggestedTokens.push({ type: "unchanged", value: val });
      diffTokens.push({ type: "unchanged", value: val });
      i++;
      j++;
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      const val = origTokens[i];
      originalTokens.push({ type: "removed", value: val });
      diffTokens.push({ type: "removed", value: val });
      i++;
    } else {
      const val = suggTokens[j];
      suggestedTokens.push({ type: "added", value: val });
      diffTokens.push({ type: "added", value: val });
      j++;
    }
  }

  while (i < n) {
    const val = origTokens[i];
    originalTokens.push({ type: "removed", value: val });
    diffTokens.push({ type: "removed", value: val });
    i++;
  }

  while (j < m) {
    const val = suggTokens[j];
    suggestedTokens.push({ type: "added", value: val });
    diffTokens.push({ type: "added", value: val });
    j++;
  }

  return { originalTokens, suggestedTokens, diffTokens };
}
