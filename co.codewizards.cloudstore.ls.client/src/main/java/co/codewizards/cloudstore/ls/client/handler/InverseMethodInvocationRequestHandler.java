package co.codewizards.cloudstore.ls.client.handler;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.InvocationType;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;

public class InverseMethodInvocationRequestHandler extends AbstractInverseServiceRequestHandler<InverseMethodInvocationRequest, InverseMethodInvocationResponse> {

	@Override
	public InverseMethodInvocationResponse handle(final InverseMethodInvocationRequest request) {
		assertNotNull("request", request);
		final MethodInvocationRequest methodInvocationRequest = request.getMethodInvocationRequest();

		final ObjectManager objectManager = getLocalServerClient().getObjectManager();
		final ClassManager classManager = objectManager.getClassManager();

		final String className = methodInvocationRequest.getClassName();
		final Class<?> clazz = className == null ? null : classManager.getClassOrFail(className);

		final String methodName = methodInvocationRequest.getMethodName();

		final Object object = methodInvocationRequest.getObject();
		if (ObjectRef.VIRTUAL_METHOD_NAME_REMOVE_OBJECT_REF.equals(methodName)) {
			objectManager.remove(object);
			return new InverseMethodInvocationResponse(request, MethodInvocationResponse.forInvocation(null));
		}

		final String[] argumentTypeNames = methodInvocationRequest.getArgumentTypeNames();
		final Class<?>[] argumentTypes = argumentTypeNames == null ? null : classManager.getClassesOrFail(argumentTypeNames);

		final Object[] arguments = methodInvocationRequest.getArguments();

		final Object resultObject;

		final InvocationType invocationType = methodInvocationRequest.getInvocationType();
		switch (invocationType) {
			case CONSTRUCTOR:
				resultObject = invokeConstructor(clazz, arguments);
				break;
			case OBJECT:
				resultObject = invoke(object, methodName, argumentTypes, arguments);
				break;
			case STATIC:
				resultObject = invokeStatic(clazz, methodName, arguments);
				break;
			default:
				throw new IllegalStateException("Unknown InvocationType: " + invocationType);
		}

		return new InverseMethodInvocationResponse(request, MethodInvocationResponse.forInvocation(resultObject));
	}
}
