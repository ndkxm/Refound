import {
  getAuth,
  getIdToken,
  GoogleAuthProvider,
  signInWithCredential,
  signOut as firebaseSignOut,
} from '@react-native-firebase/auth';
import {
  GoogleSignin,
  isErrorWithCode,
  isSuccessResponse,
  statusCodes,
} from '@react-native-google-signin/google-signin';

let configured = false;

function configure() {
  if (configured) return;
  // Web client ID (OAuth client loại 3 trong google-services.json): Google cấp idToken cho client này,
  // Firebase mới đổi được sang tài khoản Firebase. Không phải bí mật.
  GoogleSignin.configure({ webClientId: process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID });
  configured = true;
}

/**
 * Đăng nhập Google → Firebase Auth → Firebase ID token để gửi `POST /auth/firebase` (ADR-05).
 * Trả `null` khi người dùng tự đóng hộp chọn tài khoản.
 */
export async function getFirebaseIdToken(): Promise<string | null> {
  configure();
  try {
    await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });
    const res = await GoogleSignin.signIn();
    if (!isSuccessResponse(res)) return null;
    const googleIdToken = res.data.idToken;
    if (!googleIdToken)
      throw new Error('Google không trả idToken: kiểm tra EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID.');

    const { user } = await signInWithCredential(
      getAuth(),
      GoogleAuthProvider.credential(googleIdToken),
    );
    const idToken = await getIdToken(user);
    if (__DEV__) logClaims(idToken);
    return idToken;
  } catch (e) {
    if (isErrorWithCode(e)) {
      if (e.code === statusCodes.SIGN_IN_CANCELLED || e.code === statusCodes.IN_PROGRESS)
        return null;
      if (e.code === statusCodes.PLAY_SERVICES_NOT_AVAILABLE) {
        throw new Error('Máy chưa có Google Play Services nên không đăng nhập Google được.');
      }
    }
    throw e;
  }
}

/** Thoát cả Google (lần sau được chọn lại tài khoản) lẫn Firebase. Lỗi ở đây không chặn việc đăng xuất khỏi app. */
export async function signOutGoogle() {
  configure();
  await Promise.allSettled([GoogleSignin.signOut(), firebaseSignOut(getAuth())]);
}

// U18: in các claim server kiểm để đối chiếu với FirebaseTokenVerifier. Chỉ chạy ở bản dev, không in cả token.
function logClaims(idToken: string) {
  try {
    const part = idToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const claims = JSON.parse(atob(part)) as {
      iss?: string;
      aud?: string;
      firebase?: { sign_in_provider?: string };
    };
    console.log('[U18] Firebase ID token', {
      iss: claims.iss,
      aud: claims.aud,
      sign_in_provider: claims.firebase?.sign_in_provider,
    });
  } catch {
    console.log('[U18] không giải mã được ID token');
  }
}
