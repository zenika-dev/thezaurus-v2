import { GoogleLogin } from "@react-oauth/google";
import type { CredentialResponse } from "@react-oauth/google";

interface GoogleLoginButtonProps {
  onSuccess: (token: string) => void;
  onError: () => void;
}

export default function GoogleLoginButton({ onSuccess, onError }: GoogleLoginButtonProps) {
  const handleSuccess = (credentialResponse: CredentialResponse) => {
    if (credentialResponse.credential) {
      onSuccess(credentialResponse.credential);
    } else {
      onError();
    }
  };

  return (
    <div className="flex flex-col items-center justify-center space-y-4">
      <h2 className="text-xl font-bold mb-4">Connexion requise</h2>
      <GoogleLogin
        onSuccess={handleSuccess}
        onError={onError}
        useOneTap
        theme="filled_blue"
        shape="pill"
        text="signin_with"
      />
    </div>
  );
}
